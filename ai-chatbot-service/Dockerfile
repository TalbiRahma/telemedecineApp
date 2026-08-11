FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_DEFAULT_TIMEOUT=120

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --retries 10 --index-url https://download.pytorch.org/whl/cpu "torch>=2.2,<3"
RUN pip install --no-cache-dir --retries 10 -r requirements.txt
COPY app ./app
COPY faiss_index ./faiss_index
RUN mkdir -p /app/sessions

EXPOSE 8000
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=8 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=3)" || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
