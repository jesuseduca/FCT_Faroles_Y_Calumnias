import asyncio
import random
import json
import database

salas = {}
DURACION_DIALOGO = 60  # segundos

def generar_codigo():
    letras = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    codigo = ""
    for i in range(6):
        codigo += random.choice(letras)
    return codigo

async def crear_sala(ws, datos):
    nombre_jugador = datos["datos"]["nombre_usuario"]
    perfil_id = datos["datos"].get("perfil_id", None)

    codigo = generar_codigo()
    while codigo in salas:
        codigo = generar_codigo()

    salas[codigo] = {
        "host": ws,
        "jugadores": {ws: nombre_jugador},
        "perfiles": {}
    }

    if perfil_id:
        salas[codigo]["perfiles"][nombre_jugador] = perfil_id

    await ws.send(json.dumps({"tipo": "sala_creada", "codigo": codigo}))

    await ws.send(json.dumps({
        "tipo": "jugador_unido",
        "nombre": nombre_jugador,
        "jugadores": [nombre_jugador]
    }))

    lista = database.obtener_colecciones()
    await ws.send(json.dumps({"tipo": "lista_colecciones", "colecciones": lista}))

async def unirse_sala(ws, datos):
    codigo = datos["datos"]["codigo"]
    nombre_jugador = datos["datos"]["nombre_usuario"]
    perfil_id = datos["datos"].get("perfil_id", None)

    if codigo not in salas:
        await ws.send(json.dumps({"tipo": "error", "mensaje": "Sala no encontrada"}))
        return

    salas[codigo]["jugadores"][ws] = nombre_jugador

    if perfil_id:
        salas[codigo]["perfiles"][nombre_jugador] = perfil_id

    for jugador_ws in salas[codigo]["jugadores"]:
        respuesta = {
            "tipo": "jugador_unido",
            "nombre": nombre_jugador,
            "jugadores": list(salas[codigo]["jugadores"].values())
        }
        await jugador_ws.send(json.dumps(respuesta))

async def cerrar_lobby(ws, datos):
    codigo = datos["datos"]["codigo"]
    coleccion_id = datos["datos"]["coleccion_id"]
    coleccion = database.obtener_coleccion(coleccion_id)
    palabra = random.choice(coleccion["palabras"])
    jugadores = list(salas[codigo]["jugadores"].values())
    farol = random.choice(jugadores)

    palabras_señuelo = random.sample(coleccion["palabras"], 12)
    if palabra not in palabras_señuelo:
        palabras_señuelo[0] = palabra
    random.shuffle(palabras_señuelo)

    salas[codigo]["votos"] = {}
    salas[codigo]["vidas"] = {}
    salas[codigo]["partida_iniciada"] = True
    salas[codigo]["coleccion_id"] = coleccion_id
    for nombre in salas[codigo]["jugadores"].values():
        salas[codigo]["vidas"][nombre] = 3

    for jugador_ws, nombre in salas[codigo]["jugadores"].items():
        es_farol = nombre == farol
        await jugador_ws.send(json.dumps({
            "tipo": "partida_iniciada",
            "es_farol": es_farol,
            "palabra": palabra,
            "palabras": palabras_señuelo
        }))

    lista_jugadores = []
    for nombre in salas[codigo]["jugadores"].values():
        lista_jugadores.append({"nombre": nombre, "vidas": 3})

    for jugador_ws in salas[codigo]["jugadores"]:
        await jugador_ws.send(json.dumps({
            "tipo": "lista_jugadores",
            "jugadores": lista_jugadores
        }))

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

    # Si la partida ya empezó no hacemos nada más, el juego continúa sin él
    if salas[codigo_sala].get("partida_iniciada", False):
        return

    # Si era el host y la partida no ha empezado, borramos la sala y avisamos a todos
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