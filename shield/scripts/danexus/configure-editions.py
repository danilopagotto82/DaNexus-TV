from pathlib import Path
import re
fire=Path(__file__).resolve().parents[2]
shield=fire.parent/'DaNexus-Shield'
for root, edition, appid in [(fire,'Fire TV','com.danexus.tv.fire'),(shield,'Shield','com.danexus.tv.shield')]:
 p=root/'app/build.gradle.kts'; s=p.read_text(encoding='utf-8')
 s=re.sub(r'applicationId = "[^"]+"',lambda m:'applicationId = "'+('com.danexus.app.'+edition.split()[0].lower() if '.app' in m.group() else appid)+'"',s)
 # Remove inherited explicit debug IDs; rely on base applicationId + debug suffix.
 s=re.sub(r'androidComponents \{\s*onVariants\(selector\(\).withBuildType\("debug"\)\).*?\n\}\n','',s,flags=re.S)
 s=re.sub(r'resValue\("string", "app_name", "[^"]*"\)','resValue("string", "app_name", "DaNexus '+edition+'")',s)
 if 'resValue("string", "app_name"' not in s.split('buildTypes')[0]:
  s=s.replace('        applicationId = "'+appid+'"','        applicationId = "'+appid+'"\n        resValue("string", "app_name", "DaNexus '+edition+'")',1)
 p.write_text(s,encoding='utf-8')
p=shield/'app/src/main/java/com/nuvio/tv/ui/screens/settings/AboutScreen.kt'
s=p.read_text(encoding='utf-8')
if 'DaNexusRemotePanel()' not in s: s=s.replace('                MemberBrandWordmark(','                DaNexusRemotePanel()\n\n                MemberBrandWordmark(',1)
assert 'DaNexusRemotePanel()' in s
p.write_text(s,encoding='utf-8')
p=fire/'app/src/main/java/com/nuvio/tv/MainActivity.kt'; s=p.read_text(encoding='utf-8')
start=s.index('private fun ModernSidebarScaffold(')
head,body=s[:start],s[start:]
# Lightweight top navigation uses official routes and existing controller; no new engine.
anchor='        if (showSidebar && (sidebarVisible || sidebarShowExpandedPanel)) {'
assert anchor in body
if 'DaNexus Fire TV top navigation' not in body:
 bar='''        // DaNexus Fire TV top navigation: native focusable buttons without blur or video.
        if (showSidebar) {
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DaNexus", modifier = Modifier.padding(end = 12.dp))
                drawerItems.forEach { item ->
                    androidx.tv.material3.Button(onClick = {
                        keyboardController?.hide()
                        onNavigate(item.route)
                        navigateToDrawerRoute(navController, currentRoute, item.route)
                    }) { Text(item.label) }
                }
                if (showProfileSelector) {
                    androidx.tv.material3.Button(onClick = onSwitchProfile) { Text(activeProfileName) }
                }
            }
        }

'''
 body=body.replace(anchor,bar+anchor,1)
 # Reserve space above content on root pages only.
 needle='        Box(\n            modifier = Modifier\n                .fillMaxSize()'
 assert needle in body
 body=body.replace(needle,needle+'\n                .padding(top = if (showSidebar) 64.dp else 0.dp)',1)
 s=head+body
p.write_text(s,encoding='utf-8')
print('Distinct app packages, Shield QR and Fire TV native top navigation configured')
