import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs';import os from 'node:os';import path from 'node:path';
import {createSmokeServer} from './smoke-server.mjs';
test('smoke HTTP fornece 404, sem frame, Range válido e só eventos autorizados',async()=>{
 const dir=fs.mkdtempSync(path.join(os.tmpdir(),'danexus-smoke-')),media=path.join(dir,'good.mp4');fs.writeFileSync(media,Buffer.from('fake-range-fixture'));
 const server=createSmokeServer({baseUrl:'http://127.0.0.1',mediaPath:media});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const base=`http://127.0.0.1:${server.address().port}`;
 try {
 assert.equal((await fetch(base+'/manifest.json').then(r=>r.json())).name,'DaNEXUS Smart Source Smoke');
 const streams=await fetch(base+'/stream/movie/danexus-smoke:http404.json').then(r=>r.json());assert.equal((await fetch(base+'/stream/movie/danexus-smoke%3Ahttp404.json').then(r=>r.json())).streams.length,2);assert.equal((await fetch(base+'/%ZZ')).status,400);assert.equal(streams.streams.length,2);assert.notEqual(streams.streams[0].url,streams.streams[1].url);
 assert.equal((await fetch(base+'/dead.mp4')).status,404);
 const timeout=await fetch(base+'/no-frame.mp4');const reader=timeout.body.getReader();await assert.rejects(Promise.race([reader.read(),new Promise((_,reject)=>setTimeout(()=>reject(Error('sem primeiro frame')),50))]));await reader.cancel();
 const range=await fetch(base+'/good.mp4',{headers:{range:'bytes=0-3'}});assert.equal(range.status,206);assert.equal(await range.text(),'fake');
 assert.equal((await fetch(base+'/good.mp4',{headers:{range:'bytes=9999-'}})).status,416);
 assert.equal((await fetch(base+'/api/smart-source/feedback',{method:'POST',body:JSON.stringify({result:'HTTP_FATAL',mirrorKey:'dead',url:'https://private/?token=secret',token:'secret'})})).status,200);
 const events=await fetch(base+'/events').then(r=>r.json());assert.ok(!JSON.stringify(events).includes('secret'));assert.ok(events.events.some(e=>e.type==='playback'&&e.result==='HTTP_FATAL'));
 }finally{await server.stop();fs.rmSync(dir,{recursive:true,force:true});}
});
