import asyncio
import json
import websockets
import salas
import partida

async def gestionar_conexion(ws):
    try:
        async for mensaje_raw in ws:
            datos = json.loads(mensaje_raw)
            tipo  = datos["tipo"]

            if tipo == "crear_sala":
                await salas.crear_sala(ws, datos)
            elif tipo == "unirse_sala":
                await salas.unirse_sala(ws, datos)
            elif tipo == "cerrar_lobby":
                await salas.cerrar_lobby(ws, datos)
            elif tipo == "asignar_rol":
                await partida.asignar_rol(ws, datos)
            elif tipo == "jugador_listo":
                await partida.jugador_listo(ws, datos)
            elif tipo == "voto":
                await partida.voto(ws, datos)
            elif tipo == "resultado_adivinanza":
                await partida.resultado_adivinanza(ws, datos)
            elif tipo == "partida_terminada":
                await partida.partida_terminada(ws, datos)
            elif tipo == "pedir_historial":
                await partida.pedir_historial(ws, datos)
            elif tipo == "login":
                await partida.login(ws, datos)
            elif tipo == "registro":
                await partida.registro(ws, datos)
            elif tipo == "pedir_perfil":
                await partida.pedir_perfil(ws, datos)
            elif tipo == "pedir_colecciones":
                await partida.pedir_colecciones(ws, datos)
            elif tipo == "pedir_lista_jugadores":
                await partida.pedir_lista_jugadores(ws, datos, salas.salas)
            elif tipo == "guardar_partida":
                await partida.guardar_partida_jugador(ws, datos)
            else:
                await ws.send(json.dumps({"tipo": "error", "mensaje": "Tipo desconocido"}))

    finally:
        # Cuando el cliente se desconecta, sea como sea, limpiamos su sala
        await salas.jugador_desconectado(ws)


async def main():
    async with websockets.serve(gestionar_conexion, "0.0.0.0", 8765):
        print("Servidor arrancado en el puerto 8765")
        await asyncio.Future()

asyncio.run(main())