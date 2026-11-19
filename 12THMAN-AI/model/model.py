"""
12THMAN GPT-1 Architecture
Full GPT-style decoder-only Transformer implemented in pure PyTorch from scratch.
Zero pretrained weights. All weights randomly initialized.
"""

import math
import torch
import torch.nn as nn
import torch.nn.functional as F
from typing import Optional, Tuple
from model.config import GPTConfig


class CausalSelfAttention(nn.Module):
    """
    Multi-head causal self-attention with causal triangular mask.
    """
    def __init__(self, config: GPTConfig):
        super().__init__()
        assert config.n_embd % config.n_head == 0
        self.n_head = config.n_head
        self.n_embd = config.n_embd
        self.head_dim = config.n_embd // config.n_head
        self.dropout = config.dropout

        # Key, Query, Value projections combined into one linear layer
        self.c_attn = nn.Linear(config.n_embd, 3 * config.n_embd, bias=config.bias)
        # Output projection
        self.c_proj = nn.Linear(config.n_embd, config.n_embd, bias=config.bias)
        # Regularization
        self.attn_dropout = nn.Dropout(config.dropout)
        self.resid_dropout = nn.Dropout(config.dropout)

        # Causal mask to ensure attention is only directed to previous tokens
        self.register_buffer(
            "bias",
            torch.tril(torch.ones(config.block_size, config.block_size)).view(
                1, 1, config.block_size, config.block_size
            )
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        B, T, C = x.size()  # batch size, sequence length, embedding dimensionality (n_embd)

        # Calculate query, key, values for all heads in batch and move head forward to the batch dim
        q, k, v = self.c_attn(x).split(self.n_embd, dim=2)
        k = k.view(B, T, self.n_head, self.head_dim).transpose(1, 2)  # (B, nh, T, hs)
        q = q.view(B, T, self.n_head, self.head_dim).transpose(1, 2)  # (B, nh, T, hs)
        v = v.view(B, T, self.n_head, self.head_dim).transpose(1, 2)  # (B, nh, T, hs)

        # Scaled dot-product attention
        att = (q @ k.transpose(-2, -1)) * (1.0 / math.sqrt(k.size(-1)))
        att = att.masked_fill(self.bias[:, :, :T, :T] == 0, float("-inf"))
        att = F.softmax(att, dim=-1)
        att = self.attn_dropout(att)
        y = att @ v  # (B, nh, T, hs)
        y = y.transpose(1, 2).contiguous().view(B, T, C)  # re-assemble all head outputs side by side

        # Output projection
        y = self.resid_dropout(self.c_proj(y))
        return y


class MLP(nn.Module):
    """
    Position-wise Feed-Forward Network.
    """
    def __init__(self, config: GPTConfig):
        super().__init__()
        self.c_fc = nn.Linear(config.n_embd, 4 * config.n_embd, bias=config.bias)
        self.gelu = nn.GELU()
        self.c_proj = nn.Linear(4 * config.n_embd, config.n_embd, bias=config.bias)
        self.dropout = nn.Dropout(config.dropout)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = self.c_fc(x)
        x = self.gelu(x)
        x = self.c_proj(x)
        x = self.dropout(x)
        return x


class TransformerBlock(nn.Module):
    """
    Transformer Decoder Block with pre-LayerNorm and residual connections.
    """
    def __init__(self, config: GPTConfig):
        super().__init__()
        self.ln_1 = nn.LayerNorm(config.n_embd)
        self.attn = CausalSelfAttention(config)
        self.ln_2 = nn.LayerNorm(config.n_embd)
        self.mlp = MLP(config)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # Pre-LayerNorm residual stream
        x = x + self.attn(self.ln_1(x))
        x = x + self.mlp(self.ln_2(x))
        return x


class TwelveManGPT(nn.Module):
    """
    12THMAN GPT-1 Language Model
    Decoder-only Transformer trained from scratch.
    """
    def __init__(self, config: GPTConfig):
        super().__init__()
        self.config = config

        self.transformer = nn.ModuleDict(dict(
            wte = nn.Embedding(config.vocab_size, config.n_embd),
            wpe = nn.Embedding(config.block_size, config.n_embd),
            drop = nn.Dropout(config.dropout),
            h = nn.ModuleList([TransformerBlock(config) for _ in range(config.n_layer)]),
            ln_f = nn.LayerNorm(config.n_embd)
        ))

        self.lm_head = nn.Linear(config.n_embd, config.vocab_size, bias=False)

        # Weight tying: share weights between input token embeddings and final linear projection
        self.transformer.wte.weight = self.lm_head.weight

        # Initialize all weights randomly with standard normal distribution
        self.apply(self._init_weights)

        # Report number of parameters
        n_params = self.get_num_params()
        print(f"[12THMAN GPT-1] Model initialized with {n_params:,} parameters (all randomly initialized).")

    def get_num_params(self, non_embedding=False):
        """
        Return the number of parameters in the model.
        """
        n_params = sum(p.numel() for p in self.parameters())
        if non_embedding:
            n_params -= self.transformer.wpe.weight.numel()
        return n_params

    def _init_weights(self, module):
        """
        Random weight initialization following standard GPT initialization scheme.
        Zero pretrained weights.
        """
        if isinstance(module, nn.Linear):
            torch.nn.init.normal_(module.weight, mean=0.0, std=0.02)
            if module.bias is not None:
                torch.nn.init.zeros_(module.bias)
        elif isinstance(module, nn.Embedding):
            torch.nn.init.normal_(module.weight, mean=0.0, std=0.02)
        elif isinstance(module, nn.LayerNorm):
            torch.nn.init.zeros_(module.bias)
            torch.nn.init.ones_(module.weight)

    def forward(
        self,
        idx: torch.Tensor,
        targets: Optional[torch.Tensor] = None
    ) -> Tuple[torch.Tensor, Optional[torch.Tensor]]:
        device = idx.device
        b, t = idx.size()
        assert t <= self.config.block_size, f"Cannot forward sequence of length {t}, block size is {self.config.block_size}"

        # Position indices: [0, 1, 2, ..., t-1]
        pos = torch.arange(0, t, dtype=torch.long, device=device)

        # Token + Positional embeddings
        tok_emb = self.transformer.wte(idx)           # (b, t, n_embd)
        pos_emb = self.transformer.wpe(pos)           # (t, n_embd)
        x = self.transformer.drop(tok_emb + pos_emb)  # (b, t, n_embd)

        # Forward through decoder blocks
        for block in self.transformer.h:
            x = block(x)

        # Final LayerNorm
        x = self.transformer.ln_f(x)

        if targets is not None:
            # If targets are given, calculate cross-entropy loss
            logits = self.lm_head(x)  # (b, t, vocab_size)
            loss = F.cross_entropy(logits.view(-1, logits.size(-1)), targets.view(-1), ignore_index=-1)
        else:
            # Inference-time optimization: only forward the lm_head on the very last position
            logits = self.lm_head(x[:, [-1], :])  # (b, 1, vocab_size)
            loss = None

        return logits, loss

    @torch.no_grad()
    def generate(
        self,
        idx: torch.Tensor,
        max_new_tokens: int = 64,
        temperature: float = 0.8,
        top_k: Optional[int] = 40,
        eos_token_id: Optional[int] = None
    ) -> torch.Tensor:
        """
        Autoregressive text generation.
        """
        for _ in range(max_new_tokens):
            # If sequence context exceeds block_size, crop it
            idx_cond = idx if idx.size(1) <= self.config.block_size else idx[:, -self.config.block_size:]
            # Forward pass to get logits for the last token
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :] / max(temperature, 1e-5)

            # Optional top-k filtering
            if top_k is not None and top_k > 0:
                v, _ = torch.topk(logits, min(top_k, logits.size(-1)))
                logits[logits < v[:, [-1]]] = -float("Inf")

            # Softmax to obtain probabilities
            probs = F.softmax(logits, dim=-1)
            # Sample next token
            idx_next = torch.multinomial(probs, num_samples=1)
            # Append next token to sequence
            idx = torch.cat((idx, idx_next), dim=1)

            # Stop if EOS token generated
            if eos_token_id is not None and (idx_next == eos_token_id).all():
                break

        return idx
