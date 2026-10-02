# Local ADB client provenance

Upstream: https://github.com/MuntashirAkon/libadb-android/tree/3.1.1
Commit: c849886ebc6d48e7b46d967e78a6bb65c90c3b74.
The Java sources are retained under their original headers and licenses.
ColorGoogle uses the Apache-2.0 option of the dual license; bundled third-party
BSD/MIT notices are retained. Dependencies retain their own licenses.

Local changes: bound socket connect/read/TLS handshake timeouts for the local
pairing and activation operations; close incomplete pairing sockets safely;
create SSL contexts per identity rather than caching a previous pairing key.
Treat peer stream closure as EOF and wake waiting readers on closure.
The ADB protocol and cryptographic pairing algorithms are unchanged.

This module only connects to the phone's loopback address in ColorGoogle. It
does not scan hosts or expose an arbitrary command interface to other apps.
