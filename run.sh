# Generate load on the application
pizza-store-ip=34.82.176.152
ddosify -config pizza-store/ddosify_order.json


# Run flagd locally
docker run \
  --name flagd \
  -p 8013:8013 \
  -v $(pwd):/pizza-store/flagd \
  ghcr.io/open-feature/flagd:latest start \
  --uri file:pizza-store/flagd/sampleFlag.flagd.json

# Run flagd locally (worked better*)
flagd start \
  --port 8013 \
  --uri file:./pizza-store/flagd/example_flags.flagd.json --cors-origin * --debug


# Build dockerfiles
docker buildx build --platform linux/amd64 -t alicejgibbons:latest .

# Deploy on K8s

kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.2/cert-manager.yaml && \
kubectl wait --timeout=60s --for condition=Available=True deploy --all -n 'cert-manager'

kubectl create namespace open-feature-operator-system

kubectl apply -f https://github.com/open-feature/open-feature-operator/releases/download/v0.5.4/release.yaml && \
kubectl wait --timeout=60s --for condition=Available=True deploy --all -n 'open-feature-operator-system'