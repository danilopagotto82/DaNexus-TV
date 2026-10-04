import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const dir=path.dirname(fileURLToPath(import.meta.url));
export function createSmokeServer({baseUrl,mediaPath=path.join(dir,'good.mp4'),stallPath=path.join(dir,'stall.ts'),onEvent=()=>{}}={}) {
  const ids=['http404','first-frame-timeout','stall','exhausted'];
  const labels=['01 · Fonte 404 → vídeo válido','02 · Sem primeiro frame → vídeo válido','03 · Travamento → vídeo válido','04 · Todas ruins → fim da espera'];
  const manifest={id:'danexus.phase06.smoke',version:'1.0.0',name:'DaNEXUS Smart Source Smoke',description:'Prova controlada da FASE 06; sem fontes privadas.',resources:['catalog','meta','stream'],types:['movie'],catalogs:[{type:'movie',id:'danexus-phase06',name:'FASE 06 · testes controlados'}]};
  const events=[];const sockets=new Set();
  const server=http.createServer(async(req,res)=>{
    let pathname;
    try { pathname=decodeURIComponent(new URL(req.url,baseUrl).pathname); }
    catch { res.writeHead(400);res.end();return; }
    const json=(status,body)=>{res.writeHead(status,{'content-type':'application/json; charset=utf-8','access-control-allow-origin':'*'});res.end(JSON.stringify(body));};
    const event=(body)=>{const row={at:new Date().toISOString(),...body};events.push(row);if(events.length>200)events.shift();onEvent(row);};
    if(req.method==='GET'&&pathname==='/manifest.json')return json(200,manifest);
    if(req.method==='GET'&&pathname==='/health')return json(200,{ok:true,phase:'06'});
    if(req.method==='GET'&&pathname==='/NUVIO_DANEXUS_FASE06.apk'){
      const apk=path.resolve(dir,'../../../entrega/NUVIO_DANEXUS_FASE06.apk');
      if(!fs.existsSync(apk))return json(503,{error:'APK ainda não disponível.'});
      res.writeHead(200,{'content-type':'application/vnd.android.package-archive','content-length':fs.statSync(apk).size,'content-disposition':'attachment; filename=NUVIO_DANEXUS_FASE06.apk'});
      const file=fs.createReadStream(apk);res.on('close',()=>file.destroy());return file.pipe(res);
    }
    if(req.method==='GET'&&pathname==='/events')return json(200,{events});
    if(req.method==='GET'&&pathname==='/catalog/movie/danexus-phase06.json')return json(200,{metas:ids.map((id,i)=>({id:'danexus-smoke:'+id,type:'movie',name:labels[i]}))});
    const meta=pathname.match(/^\/meta\/movie\/danexus-smoke:([^/]+)\.json$/);
    if(meta&&ids.includes(meta[1]))return json(200,{meta:{id:'danexus-smoke:'+meta[1],type:'movie',name:labels[ids.indexOf(meta[1])],description:'Abra a primeira opção uma vez. O vídeo de prova mostra cores em movimento.'}});
    const stream=pathname.match(/^\/stream\/movie\/danexus-smoke:([^/]+)\.json$/);
    if(stream&&ids.includes(stream[1])){
      const bad=stream[1]==='http404'?'/dead.mp4':stream[1]==='stall'?'/stall.ts':'/no-frame.mp4';
      const source=(suffix,title,mirror)=>({name:'DaNEXUS · Prova FASE 06',title,url:baseUrl+suffix,danexusSmartSource:{addonId:'phase06-smoke',mirrorKey:mirror,timeoutMs:4000}});
      return json(200,{streams:stream[1]==='exhausted'?[source('/dead.mp4','1 · 404 esperado','dead'),source('/no-frame.mp4','2 · timeout esperado','timeout')]:[source(bad,'1 · Fonte ruim de propósito','bad'),source('/good.mp4','2 · Vídeo válido','good')]});
    }
    if(pathname==='/api/smart-source/queue'&&req.method==='POST'){
      const body=await readJson(req);if(!body)return json(400,{ok:false});
      const rows=Array.isArray(body.sources)?body.sources:[];
      return json(200,{ok:true,queue:rows.slice(0,8).map(s=>({id:String(s.id||'').slice(0,64),smartScore:70,timeoutMs:4000}))});
    }
    if(pathname==='/api/smart-source/feedback'&&req.method==='POST'){
      const body=await readJson(req);if(!body)return json(400,{ok:false});
      const allowed=new Set(['FIRST_FRAME_FAST','FIRST_FRAME_SLOW','HTTP_FATAL','NETWORK_TIMEOUT','CODEC_ERROR','STALL','PLAYBACK_OK']);
      if(!allowed.has(body.result))return json(400,{ok:false});
      event({type:'playback',result:body.result,firstFrameMs:finite(body.firstFrameMs),stallMs:finite(body.stallMs),httpStatus:finite(body.httpStatus),mirrorKey:/^[\w.:-]{1,100}$/.test(body.mirrorKey||'')?body.mirrorKey:null});
      return json(200,{ok:true,controlledSmoke:true});
    }
    if(pathname==='/dead.mp4'){event({type:'request',source:'dead',status:404});res.writeHead(404);return res.end();}
    if(pathname==='/no-frame.mp4'){event({type:'request',source:'timeout'});res.writeHead(200,{'content-type':'video/mp4','content-length':'99999999'});res.flushHeaders();return;}
    if(pathname==='/stall.ts'){
      if(!fs.existsSync(stallPath))return json(503,{error:'Gere o vídeo de teste primeiro.'});
      event({type:'request',source:'stall'});res.writeHead(200,{'content-type':'video/mp2t'});res.flushHeaders();
      const data=fs.readFileSync(stallPath);let offset=0;const timer=setInterval(()=>{if(offset<data.length){const end=Math.min(offset+32*188,data.length);res.write(data.subarray(offset,end));offset=end;}else clearInterval(timer);},60);res.on('close',()=>clearInterval(timer));return;
    }
    if(pathname==='/good.mp4'){
      if(!fs.existsSync(mediaPath))return json(503,{error:'Gere o vídeo de teste primeiro.'});
      const size=fs.statSync(mediaPath).size;const range=/^bytes=(\d+)-(\d*)$/.exec(req.headers.range||'');
      let start=range?Number(range[1]):0,end=range&&range[2]?Number(range[2]):size-1;
      if(start>=size||start>end){res.writeHead(416,{'content-range':`bytes */${size}`});return res.end();}
      end=Math.min(end,size-1);const headers={'content-type':'video/mp4','accept-ranges':'bytes','content-length':end-start+1};
      if(range)headers['content-range']=`bytes ${start}-${end}/${size}`;
      event({type:'request',source:'good',status:range?206:200});res.writeHead(range?206:200,headers);
      if(req.method==='HEAD')return res.end();const data=fs.createReadStream(mediaPath,{start,end});res.on('close',()=>data.destroy());return data.pipe(res);
    }
    return json(404,{error:'Rota não encontrada.'});
  });
  server.on('connection',socket=>{sockets.add(socket);socket.on('close',()=>sockets.delete(socket));});
  server.stop=()=>{for(const s of sockets)s.destroy();return new Promise(resolve=>server.close(resolve));};
  return server;
}
function finite(v){return typeof v==='number'&&Number.isFinite(v)?v:null;}
async function readJson(req){let size=0,parts=[];try{for await(const part of req){size+=part.length;if(size>16384)return null;parts.push(part);}return JSON.parse(Buffer.concat(parts).toString());}catch{return null;}}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const host=process.argv[2]||'127.0.0.1',port=Number(process.argv[3]||8797),baseUrl=`http://${host}:${port}`;
  const server=createSmokeServer({baseUrl,onEvent:e=>console.log(JSON.stringify(e))});
  server.listen(port,host,()=>console.log(`FASE 06 · SMOKE PRONTO ${baseUrl}/manifest.json`));
  process.on('SIGINT',()=>server.stop().then(()=>process.exit(0)));
}
