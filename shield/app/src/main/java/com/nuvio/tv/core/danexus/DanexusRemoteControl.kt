package com.nuvio.tv.core.danexus

import android.content.Context
import com.nuvio.tv.R
import fi.iki.elonen.NanoHTTPD
import java.net.NetworkInterface
import java.net.Inet4Address
import java.net.URI
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** LAN-only controls. No content URLs, account credentials or history are exposed. */
object DanexusRemoteControl {
    val commands = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val nextSource = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    private val endpoint = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = endpoint
    private var server: RemoteServer? = null
    private var pin = ""

    @Synchronized fun start(context: Context): String? {
        val ip = localAddress() ?: return null
        if (server == null) {
            pin = (100000 + SecureRandom().nextInt(900000)).toString()
            server = runCatching {
                val logo = context.resources.openRawResource(R.drawable.danexus_wordmark).use { it.readBytes() }
                RemoteServer(pin, logo).apply { start(NanoHTTPD.SOCKET_READ_TIMEOUT, true) }
            }.getOrNull()
        }
        endpoint.value = if (server != null) "http://$ip:8790/?pin=$pin" else null
        return endpoint.value
    }

    @Synchronized fun stop() { server?.stop(); server = null; endpoint.value = null; pin = "" }

    private fun localAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress && !it.isLoopbackAddress }?.hostAddress
    }.getOrNull()

    internal class RemoteServer(private val secret: String, private val logo: ByteArray? = null) : NanoHTTPD(8790) {
        private val failures = mutableMapOf<String, Pair<Long, Int>>()
        private val allowed = setOf("up", "down", "left", "right", "ok", "back", "play", "sources")
        @Synchronized override fun serve(session: IHTTPSession): Response {
            fun reply(status: Response.Status, mime: String, body: String) =
                newFixedLengthResponse(status, mime, body).apply {
                    addHeader("Cache-Control", "no-store")
                    addHeader("X-Content-Type-Options", "nosniff")
                    addHeader("X-Frame-Options", "DENY")
                    addHeader("Referrer-Policy", "no-referrer")
                }
            val remote = session.remoteIpAddress.orEmpty()
            val privateAddress = runCatching { java.net.InetAddress.getByName(remote).isSiteLocalAddress }.getOrDefault(false)
            if (!privateAddress) return reply(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Apenas rede local")
            if (session.method == Method.GET && session.uri == "/")
                return reply(Response.Status.OK, "text/html; charset=utf-8", PAGE)
            if (session.method == Method.GET && session.uri == "/logo.png" && logo != null)
                return newFixedLengthResponse(Response.Status.OK, "image/png", java.io.ByteArrayInputStream(logo), logo.size.toLong()).apply {
                    addHeader("Cache-Control", "no-store")
                    addHeader("X-Content-Type-Options", "nosniff")
                }
            if (session.method != Method.POST || session.uri != "/command")
                return reply(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Não encontrado")
            val origin = session.headers["origin"]
            if (origin != null && runCatching { URI(origin).rawAuthority != session.headers["host"] }.getOrDefault(true))
                return reply(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Origem inválida")
            val now = System.currentTimeMillis()
            val previous = failures[remote]?.takeIf { now - it.first < 60_000L }
            if ((previous?.second ?: 0) >= 5) return reply(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Aguarde um minuto")
            if (session.headers["x-danexus-pin"] != secret) {
                if (failures.size > 64) failures.clear()
                failures[remote] = (previous?.first ?: now) to ((previous?.second ?: 0) + 1)
                return reply(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Confira o código na TV")
            }
            failures.remove(remote)
            val command = session.parameters["key"]?.firstOrNull()
            if (command !in allowed) return reply(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Comando inválido")
            if (!commands.tryEmit(command!!)) return reply(Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "Tente novamente")
            return reply(Response.Status.OK, MIME_PLAINTEXT, "OK")
        }
    }

    private val PAGE = """<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Controle DaNexus</title>
<style>*{box-sizing:border-box}body{margin:0;background:#070b17;color:#eef3ff;font:16px system-ui;min-height:100vh;padding:28px 18px}.box{max-width:440px;margin:auto}h1{font-size:30px;letter-spacing:-1px}h1 span{color:#73deff}.sub{color:#a4b6cf;line-height:1.5}input{width:100%;padding:15px;background:#17223b;border:1px solid #355a74;border-radius:16px;color:white;font:inherit}.pad{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:28px 0}button{background:linear-gradient(135deg,#172b45,#292047);color:white;border:1px solid #53719a;border-radius:20px;min-height:76px;font:600 18px system-ui;touch-action:manipulation}button:active{background:#359fb7;transform:scale(.96)}.ok{background:linear-gradient(135deg,#289eb8,#6840ad)}.actions{display:grid;grid-template-columns:1fr 1fr;gap:12px}#status{min-height:24px;color:#8bdef0;margin:18px 0}.note{font-size:13px;color:#8a9fbd;line-height:1.6}</style>
<main class="box"><img src="/logo.png" alt="DaNexus" style="width:min(100%,300px);height:auto"><h1><span>DaNexus</span> · Controle</h1><p class="sub">Seu celular vira o controle do aplicativo na TV. Mantenha o DaNexus aberto e os dois aparelhos na mesma rede Wi-Fi.</p><label for="pin">Código de conexão exibido na TV</label><input id="pin" inputmode="numeric" maxlength="6" autocomplete="off" placeholder="6 dígitos"><div class="pad"><i></i><button data-key="up">▲</button><i></i><button data-key="left">◀</button><button data-key="ok" class="ok">OK</button><button data-key="right">▶</button><i></i><button data-key="down">▼</button><i></i></div><div class="actions"><button data-key="back">Voltar</button><button data-key="play">Pausar / retomar</button><button data-key="sources" style="grid-column:span 2">Tentar próxima fonte</button></div><p id="status" aria-live="polite"></p><p class="note">Este controle atua somente no DaNexus. Não controla volume, energia ou outros aplicativos. A troca de fonte requer Fonte Inteligente ligada. O PC pode estar desligado.</p></main>
<script>const p=document.querySelector('#pin'),s=document.querySelector('#status');p.value=new URLSearchParams(location.search).get('pin')||sessionStorage.getItem('danexusPin')||'';history.replaceState({},'',location.pathname);document.querySelectorAll('[data-key]').forEach(b=>b.onclick=async()=>{sessionStorage.setItem('danexusPin',p.value);try{let r=await fetch('/command?key='+b.dataset.key,{method:'POST',headers:{'X-DaNexus-Pin':p.value}});s.textContent=r.ok?'Comando enviado':await r.text()}catch(e){s.textContent='TV indisponível. Confira a rede e abra o DaNexus.'}});</script></html>"""
}
