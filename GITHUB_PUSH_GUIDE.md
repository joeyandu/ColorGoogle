# Maintainer release procedure

1. Review source changes, licenses, privacy-sensitive files and both README languages.
2. Run unit tests, release sanity, release build/lint and a clean-source build.
3. Verify the APK signing certificate and version; record actual device acceptance.
4. Commit the reviewed source. Tag that exact commit with the release version.
5. Create the matching source archive from Git, not from the entire working folder.
6. Publish the signed APK, matching source ZIP and SHA256SUMS on the GitHub Release.
7. Download published assets and compare hashes, certificate and tag/commit identity.

Do not publish signing credentials, local.properties, delivery records or pairing data.
Do not rewrite an existing public release tag to silently replace its binary.
