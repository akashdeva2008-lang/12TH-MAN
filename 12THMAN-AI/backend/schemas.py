"""
Pydantic Schemas for 12THMAN GPT-1 FastAPI Backend.
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: str = Field(..., description="Role: 'user' or 'assistant' or 'system'")
    content: str = Field(..., description="Text content of the message")


class ChatRequest(BaseModel):
    prompt: str = Field(..., min_length=1, description="User prompt or question")
    history: Optional[List[ChatMessage]] = Field(default=None, description="Conversation history")
    temperature: Optional[float] = Field(default=0.7, ge=0.0, le=2.0, description="Sampling temperature")
    max_tokens: Optional[int] = Field(default=64, ge=1, le=256, description="Maximum new tokens to generate")
    top_k: Optional[int] = Field(default=25, ge=1, le=100, description="Top-k filtering parameter")


class ChatResponse(BaseModel):
    response: str
    model: str = "12THMAN GPT-1"
    tokens_generated: int
    latency_ms: float
    status: str = "success"


class GenerateRequest(BaseModel):
    prompt: str = Field(..., min_length=1, description="Input prompt text")
    max_tokens: Optional[int] = Field(default=50, ge=1, le=256)
    temperature: Optional[float] = Field(default=0.7, ge=0.0, le=2.0)
    top_k: Optional[int] = Field(default=25, ge=1, le=100)


class GenerateResponse(BaseModel):
    generated_text: str
    tokens_generated: int
    latency_ms: float
    model: str = "12THMAN GPT-1"


class ModelInfoResponse(BaseModel):
    name: str = "12THMAN GPT-1"
    type: str = "Custom Cricket Language Model"
    architecture: str = "Decoder-Only Transformer (GPT)"
    pretrained_weights: bool = False
    weights_origin: str = "Trained from scratch on cricket domain corpus"
    parameters: int
    vocab_size: int
    n_layer: int
    n_head: int
    n_embd: int
    block_size: int
    val_loss: float
    checkpoint_file: str


class HealthResponse(BaseModel):
    status: str = "healthy"
    model_loaded: bool
    device: str
    version: str = "1.0.0"
    vocab_size: int
