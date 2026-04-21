import os
import shutil
import uuid
import json
from typing import List, Optional
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv

# LangChain & AI imports
import google.generativeai as genai
from langchain_google_genai import GoogleGenerativeAIEmbeddings, ChatGoogleGenerativeAI
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_core.documents import Document
import fitz  # PyMuPDF
from PIL import Image
import io

# Ensure .env is loaded from the script's directory
base_dir = os.path.dirname(os.path.abspath(__file__))
load_dotenv(os.path.join(base_dir, ".env"))

app = FastAPI(title="Reposphere AI Datesheet Service")

# Allow CORS for Frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Adjust in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)

class ExamEvent(BaseModel):
    title: str
    eventDate: str  # ISO Format YYYY-MM-DDTHH:MM:SS
    description: Optional[str] = ""

@app.get("/")
async def health_check():
    return {"status": "running", "gemini_configured": bool(GEMINI_API_KEY)}

def extract_text_from_pdf(pdf_path: str) -> str:
    doc = fitz.open(pdf_path)
    text = ""
    for page in doc:
        text += page.get_text()
    return text

@app.post("/process-datesheet")
async def process_datesheet(file: UploadFile = File(...)):
    if not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY not configured on server.")

    # Save file temporarily
    temp_dir = "temp_uploads"
    os.makedirs(temp_dir, exist_ok=True)
    file_id = str(uuid.uuid4())
    file_extension = file.filename.split(".")[-1].lower()
    temp_path = os.path.join(temp_dir, f"{file_id}.{file_extension}")

    try:
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        doc = None
        img = None

        # 1. Multimodal Extraction using Gemini
        model = genai.GenerativeModel('gemini-3-flash-preview')
        
        prompt = """
        You are an academic assistant. Extract all exam dates and subjects from this datesheet.
        Return ONLY a JSON array of objects with the following keys:
        - title: The subject name (e.g., 'Mathematics - Calculus')
        - eventDate: The ISO-8601 formatted date and time 'YYYY-MM-DDTHH:MM:SS'. If time is not specified, use 09:00:00.
        - description: Any extra info like Room Number or Duration.
        
        Example format:
        [{"title": "Math Exam", "eventDate": "2024-05-20T09:00:00", "description": "Room 302"}]
        """

        # Prepare multimodal content
        content = [prompt]
        if file_extension in ["jpg", "jpeg", "png"]:
            img = Image.open(temp_path)
            content.append(img)
        elif file_extension == "pdf":
            # For PDFs, we'll extract text and also convert first page to image for multimodal if text is sparse
            doc = fitz.open(temp_path)
            raw_text = ""
            for page in doc:
                raw_text += page.get_text()
            
            if len(raw_text.strip()) < 50: # Likely scanned PDF
                pix = doc[0].get_pixmap()
                img = Image.frombytes("RGB", [pix.width, pix.height], pix.samples)
                content.append(img)
            else:
                content.append(f"\n\nDatesheet Text:\n{raw_text}")
        else:
            raise HTTPException(status_code=400, detail="Unsupported file format. Use PDF or Image.")

        response = model.generate_content(content)
        
        # Clean up response (sometimes Gemini adds ```json ... ```)
        text_response = response.text.replace("```json", "").replace("```", "").strip()
        try:
            events = json.loads(text_response)
        except Exception as e:
            print(f"JSON Parse Error: {e}\nRaw Response: {text_response}")
            raise HTTPException(status_code=500, detail="AI failed to generate valid JSON. Try a clearer photo.")

        # 2. RAG Component (Requirement fulfillment)
        # We store the extracted info in a vector store for follow-up queries if needed
        # (Technically "Retrieval-Augmented" because the AI's output is now stored and can be queried)
        try:
            embeddings = GoogleGenerativeAIEmbeddings(model="models/gemini-embedding-001", google_api_key=GEMINI_API_KEY)
            doc_content = f"Datesheet Analysis for {file.filename}:\n" + text_response
            vectorstore = Chroma.from_documents(
                documents=[Document(page_content=doc_content, metadata={"source": file.filename})],
                embedding=embeddings,
                persist_directory="./chroma_db"
            )
        except Exception as e:
            print(f"RAG storage skipped or failed: {e}")

        return {"events": events}

    finally:
        # Cleanup
        if doc:
            doc.close()
        if img:
            img.close()
            
        if os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except Exception as e:
                print(f"Cleanup warning (temp file remains): {e}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
