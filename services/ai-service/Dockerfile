# ========== Stage 1: Dependencies ==========
FROM python:3.11-slim AS deps

WORKDIR /app

# System packages needed to build some Python extensions
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Install CPU-only PyTorch FIRST to avoid pulling the 3 GB CUDA build
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

# Install the rest of the dependencies (torch>=2.1.0 is already satisfied above)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ========== Stage 2: Runtime ==========
FROM python:3.11-slim

WORKDIR /app

# curl/wget needed for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl wget \
    && rm -rf /var/lib/apt/lists/*

# Copy installed packages from deps stage
COPY --from=deps /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=deps /usr/local/bin /usr/local/bin

# Copy application source
COPY app ./app

# Python runtime settings
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
# Always use CPU (no CUDA)
ENV FORCE_CPU=true
# Cache dirs — map these to a Docker volume to persist downloaded models
ENV TRANSFORMERS_CACHE=/root/.cache/huggingface
ENV SENTENCE_TRANSFORMERS_HOME=/root/.cache/sentence_transformers

EXPOSE 8085

# Model loading on startup can take 1-2 min — give healthcheck enough start time
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=3 \
  CMD curl -sf http://localhost:8085/health || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8085"]
