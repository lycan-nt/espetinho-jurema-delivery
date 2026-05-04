#!/usr/bin/env python3
"""
Testa se a balança envia dados pela porta serial.
Uso: python3 testar_balanca_serial.py [porta] [velocidade] [--timeout segundos]
Ex.: python3 testar_balanca_serial.py /dev/cu.usbserial-140 9600
     python3 testar_balanca_serial.py /dev/cu.usbserial-140 9600 --timeout 30
"""
import sys
import signal

try:
    import serial
except ImportError:
    print("Instale o pyserial: pip3 install pyserial")
    sys.exit(1)

args = [a for a in sys.argv[1:] if not a.startswith("--")]
timeout_opt = next((int(a.split("=")[1]) for a in sys.argv[1:] if a.startswith("--timeout=")), None)
if timeout_opt is None and "--timeout" in sys.argv:
    i = sys.argv.index("--timeout")
    timeout_opt = int(sys.argv[i + 1]) if i + 1 < len(sys.argv) else 30

porta = args[0] if len(args) > 0 else "/dev/cu.usbserial-140"
velocidade = int(args[1]) if len(args) > 1 else 9600

print(f"Abrindo {porta} em {velocidade} baud...")
print("Coloque peso na balança e pressione IMPRIMIR. Ctrl+C para sair.\n")

encerrar = False

def timeout_handler(signum, frame):
    global encerrar
    encerrar = True

if timeout_opt:
    signal.signal(signal.SIGALRM, timeout_handler)
    signal.alarm(timeout_opt)
    print(f"(Encerrando em {timeout_opt} segundos)\n")

try:
    ser = serial.Serial(porta, velocidade, timeout=0.5)
except Exception as e:
    print(f"Erro ao abrir porta: {e}")
    print("Feche o navegador e outros programas que usem a balança.")
    sys.exit(1)

try:
    while not encerrar:
        if ser.in_waiting:
            dados = ser.read(ser.in_waiting)
            hex_str = " ".join(f"{b:02x}" for b in dados)
            try:
                texto = dados.decode("utf-8", errors="replace")
                print(f"Hex: {hex_str}  |  Texto: {repr(texto)}")
            except Exception:
                print(f"Hex: {hex_str}")
        else:
            ser.reset_input_buffer()
except KeyboardInterrupt:
    print("\nEncerrado.")
except Exception as e:
    print(f"\nErro: {e}")
finally:
    if timeout_opt:
        signal.alarm(0)
    ser.close()
    print("Porta fechada.")
