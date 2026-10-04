# Nuvio V2 attribution and Fire TV adaptation

DaNexus derives its optional Nuvio V2 appearance from ysosrs123/NuvioTV-Fork,
tag 1.1.0-beta-nt4, commit 26e959c978c2039ceeee62317b5e2c6e58b8cb62.
Repository: https://github.com/ysosrs123/NuvioTV-Fork
License: GNU GPL v3, as provided in this source distribution.

Reused: V2Appearance.kt appearance model, GlassMaterial.kt opacity policy,
V2Palette.kt surface palette (adapted to existing DaNexus theme identifiers).
Adapted: NuvioGlassSurface.kt static gradient, role-specific opacity and rims,
NuvioFocusSurface.kt focus decoration. Fire TV uses static performance materials
without a live blur, video capture pass or the fork's additional player engines.
The DaNexus navigation order, recommendations, source queue and local remote are
preserved. UI scale is device local, defaults to 95% and can be adjusted 85–115%.

The nt4 profile entry animation is recorded as a Shield reference for a later
phase. This Fire TV version does not port that intro animation.
