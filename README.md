# DockAI Test Application

This repository is designed to test the [DockAI](https://github.com/itzzjb/dockai) GitHub Action functionality.

## 📋 Overview

DockAI is an AI-powered Dockerfile generation framework that analyzes codebases and creates optimized, production-ready Dockerfiles. This test repository automates the testing process by:

1. Reading a target repository URL from `config.json`
2. Checking out the target repository
3. Running DockAI to generate a Dockerfile
4. Validating and storing the generated Dockerfile

## 🚀 Setup Instructions

### 1. Configure Target Repository

Edit `config.json` to specify the repository you want to generate a Dockerfile for:

```json
{
  "repository_url": "https://github.com/owner/repo-name"
}
```

**Note:** Replace `owner/repo-name` with the actual repository path (without the `https://github.com/` prefix).

### 2. Add OpenAI API Key

The GitHub Action requires an OpenAI API key to function:

1. Go to your repository **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Name: `OPENAI_API_KEY`
4. Value: Your OpenAI API key (starts with `sk-`)
5. Click **Add secret**

Alternatively, you can use other LLM providers supported by DockAI:
- Google Gemini (`GOOGLE_API_KEY`)
- Anthropic Claude (`ANTHROPIC_API_KEY`)
- Azure OpenAI (`AZURE_OPENAI_API_KEY`)
- Ollama (for local/self-hosted)

### 3. Run the Test

The workflow can be triggered in three ways:

- **Push to main branch:** Automatically runs on every push
- **Pull request:** Runs on PR creation/update
- **Manual trigger:** Go to Actions → Test DockAI Action → Run workflow

## 📄 Files

- `config.json` - Configuration file containing the target repository URL
- `.github/workflows/test-dockai.yml` - GitHub Action workflow definition
- `README.md` - This file

## 🔍 What the Workflow Does

1. **Checkout** - Clones this test repository
2. **Read Config** - Extracts the target repository URL from `config.json`
3. **Checkout Target** - Clones the target repository to `target-repo/` directory
4. **Run DockAI** - Executes the DockAI action to generate a Dockerfile
5. **Verify** - Checks if the Dockerfile was created successfully
6. **Upload Artifact** - Saves the generated Dockerfile for download (available for 30 days)

## 📦 Downloading the Generated Dockerfile

After the workflow completes:

1. Go to the **Actions** tab in your repository
2. Click on the completed workflow run
3. Scroll down to **Artifacts** section
4. Download the `generated-dockerfile` artifact

## ⚙️ Customization

You can modify the workflow to:

- Change the LLM provider (see [DockAI documentation](https://github.com/itzzjb/dockai#-configuration))
- Adjust retry attempts (`max_retries`)
- Enable strict security mode (`strict_security: true`)
- Skip security scans for faster testing

Example with Google Gemini:

```yaml
- name: Run DockAI to generate Dockerfile
  uses: itzzjb/dockai@v3
  with:
    llm_provider: gemini
    google_api_key: ${{ secrets.GOOGLE_API_KEY }}
    project_path: target-repo
```

## 📚 Resources

- [DockAI Repository](https://github.com/itzzjb/dockai)
- [DockAI Documentation](https://itzzjb.github.io/dockai/)
- [GitHub Actions Guide](https://github.com/itzzjb/dockai/blob/main/docs/github-actions.md)

## 🐛 Troubleshooting

### Workflow fails with "Invalid repository_url"

Ensure `config.json` contains the repository path without the `https://github.com/` prefix:
- ✅ Correct: `"owner/repo-name"`
- ❌ Incorrect: `"https://github.com/owner/repo-name"`

### Workflow fails with API key error

Make sure you've added the correct API key as a repository secret with the exact name expected by the workflow.

### DockAI generates but validation fails

The target repository might have complex requirements. Check the workflow logs for details and consider:
- Increasing `max_retries`
- Reviewing DockAI's output for specific errors
- Checking if the target repository has special dependencies

## 📝 License

MIT License - This is a test repository for DockAI functionality.
