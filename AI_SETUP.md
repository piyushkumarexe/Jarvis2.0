# Connecting an AI planner securely

Do not paste an API key into source code, an issue, a pull request, or this chat. An Android APK is distributed to users and any key compiled into it can be extracted.

For the NVIDIA endpoint, the model identifier is `nvidia/nemotron-3-ultra-550b-a55b` and the OpenAI-compatible base URL is `https://integrate.api.nvidia.com/v1`. NVIDIA documents this model as a 561B-parameter Nemotron model, commonly referred to as the 550B model.

The app already has the `AiPlanner` provider boundary. The recommended production setup is:

```text
Android app -> your authenticated JARVIS backend -> LLM provider
```

Store the provider key only as a server-side environment variable. Configure the app with the backend URL, not the provider key.

For GitHub Actions, add these repository secrets at:

- `NVIDIA_API_KEY` — your NVIDIA API key
- `JARVIS_BACKEND_URL` — the URL of the backend that uses the key

Add them at:

`GitHub repository → Settings → Secrets and variables → Actions → New repository secret`

The APK workflow in `.github/workflows/build-apk.yml` builds and uploads a debug APK. It intentionally does not inject an LLM key into the APK.

If you want a direct provider integration for local testing, tell me which provider and API format you use (OpenAI-compatible, Gemini, Anthropic, or a self-hosted endpoint). I can add the provider client and an Android Keystore-backed runtime settings flow. A direct API key inside a release APK is not secure and should not be used for production.
