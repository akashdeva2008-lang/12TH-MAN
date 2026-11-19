"""
FastAPI Server for 12THMAN GPT-1 Cricket AI Assistant.
"""

import os
import sys
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

backend_dir = os.path.dirname(os.path.abspath(__file__))
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

from schemas import (
    ChatRequest,
    ChatResponse,
    GenerateRequest,
    GenerateResponse,
    ModelInfoResponse,
    HealthResponse
)
from service import ModelService

app = FastAPI(
    title="12THMAN GPT-1 Cricket AI API",
    description="Cricket-specialized language model built and trained from scratch with PyTorch",
    version="1.0.0"
)

# Enable CORS for Android client, emulator, and web interfaces
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize inference service singleton
model_service = ModelService.get_instance()


@app.get("/", summary="Root status")
def root():
    return {
        "app": "12THMAN GPT-1 Cricket AI Service",
        "status": "running",
        "model": "12THMAN GPT-1",
        "documentation": "/docs"
    }


@app.get("/health", response_model=HealthResponse, summary="Health Check")
def health_check():
    return HealthResponse(
        status="healthy",
        model_loaded=model_service.model is not None,
        device=model_service.device,
        version="1.0.0",
        vocab_size=model_service.tokenizer.vocab_size if model_service.tokenizer else 0
    )


@app.get("/model-info", response_model=ModelInfoResponse, summary="Model Metadata & Architecture")
def get_model_info():
    info = model_service.get_info()
    return ModelInfoResponse(**info)


@app.post("/chat", response_model=ChatResponse, summary="Cricket Assistant Chat")
def chat(request: ChatRequest):
    try:
        reply, tokens_count, latency = model_service.chat(
            user_message=request.prompt,
            history=request.history,
            max_new_tokens=request.max_tokens or 64,
            temperature=request.temperature or 0.7,
            top_k=request.top_k or 25
        )
        return ChatResponse(
            response=reply,
            model="12THMAN GPT-1",
            tokens_generated=tokens_count,
            latency_ms=round(latency, 2),
            status="success"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Generation error: {str(e)}")


@app.post("/generate", response_model=GenerateResponse, summary="Raw Text Generation")
def generate(request: GenerateRequest):
    try:
        generated_text, tokens_count, latency = model_service.generate(
            prompt=request.prompt,
            max_new_tokens=request.max_tokens or 50,
            temperature=request.temperature or 0.7,
            top_k=request.top_k or 25
        )
        return GenerateResponse(
            generated_text=generated_text,
            tokens_generated=tokens_count,
            latency_ms=round(latency, 2),
            model="12THMAN GPT-1"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Generation error: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
