package br.com.espetinhojurema.api;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sistema")
public class SistemaRestController {

    @Value("${server.port:9090}")
    private int porta;

    /**
     * Retorna os IPs IPv4 locais (não-loopback) desta máquina e a porta do servidor.
     * Usado pelo front-end para gerar o QR Code de acesso móvel.
     */
    @GetMapping("/acesso-local")
    public AcessoLocalView acessoLocal() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
            // se falhar lista fica vazia — frontend mostra aviso
        }
        return new AcessoLocalView(ips, porta);
    }

    public record AcessoLocalView(List<String> ips, int porta) {}
}
