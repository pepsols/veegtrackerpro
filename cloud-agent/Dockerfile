# Use the official lightweight Python image.
FROM python:3.11-slim

# Allow statements and log messages to immediately appear in the logs
ENV PYTHONUNBUFFERED=True \
    LLM_PROVIDER=local_openai \
    LOCAL_OPENAI_BASE_URL=http://172.28.48.1:1234/v1 \
    LOCAL_OPENAI_MODEL=meta-llama-3.1-8b-instruct

# Copy local code to the container image.
ENV APP_HOME /app
WORKDIR $APP_HOME
COPY . ./

# Install production dependencies.
RUN pip install --no-cache-dir -r requirements.txt

# Run the web service on container startup.
# Use gunicorn as the production-ready web server.
CMD exec gunicorn --bind :$PORT --workers 1 --threads 8 --timeout 0 main:app
