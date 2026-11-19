# 🏏 12THMAN GPT-1: Cricket AI Language Model Built From Scratch

**12THMAN GPT-1** is a custom, cricket-specialized decoder-only Transformer language model developed from scratch for the **12THMAN – Cricket Management Android App**.

---

## ⚡ Core Highlights

- **Zero Pretrained Weights**: Built and trained completely from scratch using PyTorch. No Llama, Mistral, Qwen, Gemini, ChatGPT, or external checkpoint weights are used. All weights are initialized randomly.
- **Custom BPE Tokenizer**: Built with custom Byte Pair Encoding (BPE), trained exclusively on a cricket domain corpus with special tokens (`<pad>`, `<unk>`, `<bos>`, `<eos>`, `<user>`, `<assistant>`, `<system>`).
- **Cricket Knowledge Base**: Trained on MCC cricket laws, batting/bowling/fielding techniques, umpiring signals, formats (T20, ODI, Test), dismissal types (LBW, bowled, caught, stumped, run out), and match situations.
- **Micro-GPT Architecture**: 4 Transformer decoder layers, 4 attention heads, 128 embedding dimensions, pre-LayerNorm, residual connections, causal self-attention masking, weight tying, and ~910,464 trainable parameters.
- **Production-Ready FastAPI Backend**: High-performance REST API with CORS support, sub-100ms inference on CPU, health monitoring, and `/chat` and `/generate` endpoints.

---

## 📐 Architecture Specification

| Component | Specification |
|---|---|
| **Architecture** | GPT-Style Decoder-Only Transformer |
| **Parameters** | 910,464 |
| **Transformer Layers** | 4 |
| **Attention Heads** | 4 (dim per head = 32) |
| **Embedding Dimension ($d_{model}$)** | 128 |
| **Context Window ($block\_size$)** | 128 tokens |
| **Vocabulary Size** | 823 tokens |
| **Weight Tying** | Input embeddings shared with final LM Head |
| **Normalization** | Pre-LayerNorm ($d=128$) |
| **Activation** | GELU (Gaussian Error Linear Unit) |
| **Dropout** | 0.1 |
| **Loss Function** | Cross-Entropy with Causal Masking |
| **Optimizer** | AdamW ($\beta_1=0.9, \beta_2=0.95$, weight decay = 0.01) |
| **Scheduler** | Cosine Annealing ($\eta_{min}=1e-4, \eta_{max}=1e-3$) |

---

## 📂 Project Structure

```
12THMAN-AI/
├── checkpoints/
│   ├── 12thman_gpt1_best.pt       # Best validation checkpoint (~11.2 MB)
│   ├── 12thman_gpt1_latest.pt     # Latest iteration checkpoint
│   ├── model_config.json          # Transformer hyperparameters
│   └── tokenizer.json             # BPE vocabulary and merge rules
├── dataset/
│   ├── cricket_knowledge.txt      # Domain knowledge text corpus
│   ├── cricket_qa.jsonl           # Curated Q&A pairs in JSONL format
│   ├── dataset_info.json          # Dataset metadata and statistics
│   └── prepare_dataset.py         # Dataset compilation script
├── tokenizer/
│   ├── tokenizer.py               # Custom BPE Tokenizer implementation
│   ├── train_tokenizer.py         # Tokenizer training script
│   └── tokenizer.json             # Trained vocabulary
├── model/
│   ├── config.py                  # GPTConfig dataclass
│   └── model.py                   # PyTorch Transformer architecture
├── training/
│   ├── dataset_loader.py          # Batching and train/val splitting
│   ├── train.py                   # Model training pipeline
│   └── evaluate.py                # Standalone evaluation & generation
├── backend/
│   ├── schemas.py                 # Pydantic request/response schemas
│   ├── service.py                 # Singleton inference engine
│   ├── main.py                    # FastAPI server
│   ├── run_server.py              # CLI server runner
│   └── test_api.py                # Unit and integration test suite
├── requirements.txt               # Dependencies
└── README.md                      # Complete documentation
```

---

## 🚀 Quickstart Guide

### 1. Install Dependencies
```bash
cd 12THMAN-AI
pip install -r requirements.txt
```

### 2. Dataset Preparation
```bash
python dataset/prepare_dataset.py
```

### 3. Train Tokenizer
```bash
python tokenizer/train_tokenizer.py
```

### 4. Train Model From Scratch
```bash
python training/train.py
```
Outputs validation loss, perplexity metrics, and automatically saves checkpoints to `checkpoints/12thman_gpt1_best.pt`.

### 5. Evaluate / Prompt Testing
```bash
python training/evaluate.py "What is an LBW?"
```

### 6. Start Inference Server
```bash
python backend/run_server.py --host 0.0.0.0 --port 8000
```

---

## 🌐 API Reference

### 1. `GET /health`
Returns system status, device, and model readiness:
```json
{
  "status": "healthy",
  "model_loaded": true,
  "device": "cpu",
  "version": "1.0.0",
  "vocab_size": 823
}
```

### 2. `GET /model-info`
Returns full architecture details:
```json
{
  "name": "12THMAN GPT-1",
  "type": "Custom Cricket Language Model",
  "architecture": "Decoder-Only Transformer (GPT)",
  "pretrained_weights": false,
  "weights_origin": "Trained from scratch on cricket domain corpus",
  "parameters": 910464,
  "vocab_size": 823,
  "n_layer": 4,
  "n_head": 4,
  "n_embd": 128,
  "block_size": 128,
  "val_loss": 2.2984,
  "checkpoint_file": "12thman_gpt1_best.pt"
}
```

### 3. `POST /chat`
Chat completion endpoint:
**Request:**
```json
{
  "prompt": "What is an LBW?",
  "temperature": 0.7,
  "max_tokens": 50
}
```
**Response:**
```json
{
  "response": "An LBW (Leg Before Wicket) is a dismissal under MCC Law 36 when the ball strikes the batsman's pad in line with the stumps.",
  "model": "12THMAN GPT-1",
  "tokens_generated": 28,
  "latency_ms": 64.2,
  "status": "success"
}
```

### 4. `POST /generate`
Autoregressive raw text generation:
**Request:**
```json
{
  "prompt": "A yorker is",
  "max_tokens": 40,
  "temperature": 0.7
}
```
