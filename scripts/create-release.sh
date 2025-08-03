#!/bin/bash

# Create Release Script for Money Transfer App
# Usage: ./scripts/create-release.sh <version>
# Example: ./scripts/create-release.sh 1.0.0

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1
TAG="v$VERSION"

echo "🚀 Creating release for version $VERSION"

# Check if we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "❌ Error: You must be on the main branch to create a release"
    exit 1
fi

# Check if working directory is clean
if [ -n "$(git status --porcelain)" ]; then
    echo "❌ Error: Working directory is not clean. Please commit or stash changes."
    exit 1
fi

# Update version in pom.xml
echo "📝 Updating version in pom.xml..."
sed -i.bak "s/<version>.*<\/version>/<version>$VERSION<\/version>/" pom.xml
rm pom.xml.bak

# Commit version change
git add pom.xml
git commit -m "Bump version to $VERSION"

# Create and push tag
echo "🏷️  Creating tag $TAG..."
git tag -a "$TAG" -m "Release $TAG"
git push origin "$TAG"

echo "✅ Release $TAG created successfully!"
echo "📦 GitHub Actions will now:"
echo "   - Build the application"
echo "   - Create a release with artifacts"
echo "   - Publish Docker image to GitHub Container Registry"
echo ""
echo "🔗 Check the release at: https://github.com/shihabcsedu09/money-transfer-app/releases"
echo "📦 Check packages at: https://github.com/shihabcsedu09/money-transfer-app/packages" 