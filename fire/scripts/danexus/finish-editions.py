from pathlib import Path
import xml.etree.ElementTree as ET, re, json
fire=Path(__file__).resolve().parents[2]
parent=fire.parent
for name,label in [('FireTV','Quarto · Fire TV Stick 4K Max · 2ª geração'),('Shield','Sala · NVIDIA Shield Pro 2017 · REMUX')]:
 r=parent/('DaNexus-'+name)
 p=r/'app/src/main/res/values-pt-rBR/strings.xml'
 s=p.read_text(encoding='utf-8')
 s=s.replace('Insira o PIN atual para remover o bloqueio.','Insira o PIN atual para remover o bloqueio de %1$s.')
 p.write_text(s,encoding='utf-8')
 p=r/'app/src/full/java/com/nuvio/tv/updater/UpdateRepository.kt'
 s=p.read_text(encoding='utf-8')
 s=s.replace('AbiSelector.chooseBestApkAsset(release.assets)','AbiSelector.chooseBestApkAsset(release.assets.filter { it.name.startsWith("DaNexus-'+name+'-", ignoreCase = true) })')
 p.write_text(s,encoding='utf-8')
 p=r/'.gitignore'
 s=p.read_text(encoding='utf-8')
 s+='\n# DaNexus local build outputs\n/*build.log\n/*serial-build.log\n/translations-pending.json\n'
 p.write_text(s,encoding='utf-8')
 p=r/'scripts/danexus/hardware.json'
 p.write_text(json.dumps({'room':'Quarto' if name=='FireTV' else 'Sala','player':'Amazon Fire TV Stick 4K Max (2nd generation)' if name=='FireTV' else 'NVIDIA Shield Pro 2017','display':'Samsung UN55MU6300' if name=='FireTV' else 'TCL 65C715','audio':'Samsung HW-Q600F' if name=='FireTV' else 'Denon AVR-S510BT','other':'LG BP550' if name=='Shield' else None,'wiring':'NOT_YET_OBSERVED','policy':'Use runtime HDMI/display/audio capabilities; never assume codecs from room inventory alone.'},ensure_ascii=False,indent=2),encoding='utf-8')
 p=r/'app/src/main/res/values-pt-rBR/danexus_brand_strings.xml'
 s=p.read_text(encoding='utf-8')
 s=s.replace('<string name="danexus_remote_title">','<string name="danexus_hardware_profile">'+label+'</string>\n    <string name="danexus_remote_title">')
 p.write_text(s,encoding='utf-8')
# Missing official AutoSync strings are in a separate upstream resource file.
vals={
'autosync_setting_title':'Sincronizar legendas automaticamente',
'autosync_setting_description':'Ajusta a legenda do addon usando as legendas embutidas no vídeo como referência.',
'autosync_tolerance_title':'Tolerância da sincronização',
'autosync_tolerance_description':'Mantém o tempo original quando a correção é menor ou igual a este valor.',
'autosync_tolerance_off':'Desligado',
'autosync_tolerance_value':'%1$d ms',
'autosync_thorough_title':'Busca detalhada de sincronização',
'autosync_thorough_description':'Compara outras legendas e faixas para encontrar uma correspondência melhor. Desligado: usa a primeira correspondência confiável. Só aplica ajustes com confiança.',
'autosync_toast_failed':'Não foi possível sincronizar esta legenda com o vídeo.',
'autosync_toast_failed_no_reference':'Este vídeo não tem legendas embutidas para comparação.',
'autosync_toast_failed_unsupported':'Este formato de legenda não é compatível com a sincronização automática.',
'autosync_label_synced':'Sincronizada automaticamente'}
dst=ET.Element('resources')
for k,v in vals.items(): ET.SubElement(dst,'string',{'name':k}).text=v
ET.indent(dst)
ET.ElementTree(dst).write(fire/'app/src/main/res/values-pt-rBR/strings_autosync_complete.xml',encoding='utf-8',xml_declaration=True)
# Check real MPV video output on the official beta.3 too; no newer event module is transplanted.
p=fire/'app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerSmartSource.kt'
s=p.read_text(encoding='utf-8')
s=s.replace('            if (session.firstFrameMs == null) {','            if (session.firstFrameMs == null && isUsingMpvEngine()) onSmartSourceMpvVideoStarted()\n            if (session.firstFrameMs == null) {',1)
s=s.replace('MPV_VIDEO_OUTPUT_RESTART','MPV_VIDEO_OUTPUT_READY')
p.write_text(s,encoding='utf-8')
print('Hardware inventory, edition-safe updates, AutoSync PT-BR and PIN placeholder repaired')
