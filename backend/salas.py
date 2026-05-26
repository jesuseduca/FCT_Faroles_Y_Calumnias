import random
import json

# Aquí guardamos todas las salas activas mientras el servidor está corriendo.
# Cuando el servidor se apaga esto se vacía, no se guarda en ningún sitio.
salas = {}

def generar_codigo():
    letras = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    codigo = ""
    for i in range(6):
        codigo += random.choice(letras)
    return codigo

async def crear_sala(ws, datos):
    nombre_jugador = datos["nombre_jugador"]
    clave = datos["clave"]

    # Generamos un código que no esté ya en uso, por si hay otras salas
    codigo = generar_codigo()
    while codigo in salas:
        codigo = generar_codigo()

    salas[codigo] = {
        "clave": clave,
        "host":  ws,
        "jugadores": {
            ws: nombre_jugador
        }
    }

    respuesta = {"tipo": "sala_creada", "codigo": codigo}
    await ws.send(json.dumps(respuesta))

async def unirse_sala(ws, datos):
    codigo         = datos["codigo"]
    clave          = datos["clave"]
    nombre_jugador = datos["nombre_jugador"]

    if codigo not in salas:
        await ws.send(json.dumps({"tipo": "error", "mensaje": "Sala no encontrada"}))
        return

    if salas[codigo]["clave"] != clave:
        await ws.send(json.dumps({"tipo": "error", "mensaje": "Clave incorrecta"}))
        return

    salas[codigo]["jugadores"][ws] = nombre_jugador

    # Avisamos a todos los que ya están en la sala de que llegó alguien nuevo
    for jugador_ws in salas[codigo]["jugadores"]:
        respuesta = {
            "tipo":      "jugador_unido",
            "nombre":    nombre_jugador,
            "jugadores": list(salas[codigo]["jugadores"].values())
        }
        await jugador_ws.send(json.dumps(respuesta))

async def cerrar_lobby(ws, datos):
    codigo = datos["codigo"]

    # Comprobamos que la sala existe
    if codigo not in salas:
        await ws.send(json.dumps({"tipo": "error", "mensaje": "Sala no encontrada"}))
        return

    # Solo el host puede cerrar el lobby
    if salas[codigo]["host"] != ws:
        await ws.send(json.dumps({"tipo": "error", "mensaje": "Solo el host puede cerrar el lobby"}))
        return

    # Avisamos a todos los jugadores antes de borrar la sala
    for jugador_ws in salas[codigo]["jugadores"]:
        await jugador_ws.send(json.dumps({"tipo": "lobby_cerrado"}))

    del salas[codigo]

async def jugador_desconectado(ws):
    # Buscamos en qué sala estaba este jugador
    codigo_sala = None
    for codigo, sala in salas.items():
        if ws in sala["jugadores"]:
            codigo_sala = codigo
            break

    # Si no estaba en ninguna sala no hacemos nada
    if codigo_sala is None:
        return

    nombre = salas[codigo_sala]["jugadores"][ws]
    del salas[codigo_sala]["jugadores"][ws]

    # Si la sala queda vacía la borramos
    if len(salas[codigo_sala]["jugadores"]) == 0:
        del salas[codigo_sala]
        return

    # Si era el host, borramos la sala y avisamos a todos

    if salas[codigo_sala]["host"] == ws:
        for jugador_ws in salas[codigo_sala]["jugadores"]:
             await jugador_ws.send(json.dumps({"tipo": "lobby_cerrado"}))
        del salas[codigo_sala]
        return

    # Avisamos al resto de que se fue
    for jugador_ws in salas[codigo_sala]["jugadores"]:
        respuesta = {
            "tipo":      "jugador_desconectado",
            "nombre":    nombre,
            "jugadores": list(salas[codigo_sala]["jugadores"].values())
        }
        await jugador_ws.send(json.dumps(respuesta))