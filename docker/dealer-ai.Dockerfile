ARG DOCKERHUB_LIBRARY_PREFIX=
ARG PYTHON_VERSION=3.13

FROM ${DOCKERHUB_LIBRARY_PREFIX}python:${PYTHON_VERSION}-slim-bookworm AS build
WORKDIR /workspace/dealer-ai

ARG PYPI_INDEX_URL=https://pypi.tuna.tsinghua.edu.cn/simple
ENV PIP_INDEX_URL=${PYPI_INDEX_URL} \
    UV_LINK_MODE=copy

RUN python -m pip install --no-cache-dir "uv==0.11.25"

COPY dealer-ai/pyproject.toml dealer-ai/uv.lock ./
RUN uv export --locked --no-dev --no-emit-project --format requirements-txt \
      --output-file /tmp/requirements.txt >/dev/null \
    && uv venv /opt/venv \
    && UV_DEFAULT_INDEX="${PYPI_INDEX_URL}" uv pip install \
      --python /opt/venv/bin/python \
      --requirement /tmp/requirements.txt

FROM ${DOCKERHUB_LIBRARY_PREFIX}python:${PYTHON_VERSION}-slim-bookworm AS runtime
WORKDIR /app

ENV PATH=/opt/venv/bin:${PATH} \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    TZ=Asia/Shanghai

RUN groupadd --system dealer-ai \
    && useradd --system --gid dealer-ai --home-dir /app --shell /usr/sbin/nologin dealer-ai

COPY --from=build /opt/venv /opt/venv
COPY --chown=dealer-ai:dealer-ai dealer-ai/app /app/app

USER dealer-ai

EXPOSE 8091

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8091/ready', timeout=2).read()"]

ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8091"]
