(function(){
'use strict';
var feedback=document.getElementById('feedback');
function say(text){feedback.textContent=text;}
function copy(text){if(navigator.clipboard&&window.isSecureContext){return navigator.clipboard.writeText(text);}return new Promise(function(resolve,reject){var field=document.createElement('textarea');field.value=text;field.style.cssText='position:fixed;top:0;left:0;opacity:0';document.body.appendChild(field);field.select();try{if(document.execCommand('copy'))resolve();else reject(new Error());}catch(e){reject(e);}field.remove();});}
document.querySelector('[data-copy]').addEventListener('click',function(){var button=this;copy(button.getAttribute('data-copy')).then(function(){button.querySelector('span').textContent='Copiado';say('Código copiado. Use no Downloader da sua TV.');setTimeout(function(){button.querySelector('span').textContent='Copiar';},2500);}).catch(function(){say('Toque e segure no código para copiar.');});});
document.getElementById('share').addEventListener('click',function(){var title=this.getAttribute('data-title'),url=document.querySelector('link[rel="canonical"]').href;if(navigator.share){navigator.share({title:title,text:'Veja como baixar e instalar o '+title+'.',url:url}).catch(function(e){if(e.name!=='AbortError')say('Use o botão de WhatsApp abaixo para enviar a página.');});}else{copy(url).then(function(){say('Link copiado. Cole na conversa com sua família.');}).catch(function(){say('Copie o endereço desta página ou use o botão de WhatsApp.');});}});
}());
