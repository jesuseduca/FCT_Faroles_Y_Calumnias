import asyncio
import random
import json
from salas import salas
import database


DURACION_DIALOGO = 60  # segundos


async def asignar_rol(ws, datos):
    codigo = datos["codigo"]
    # Android nos manda la lista de jugadores vivos
    vivos  = datos["vivos"]

    # Elegimos un farol aleatorio de la lista de vivos
    farol = random.choice(vivos)

    # Avisamos a cada jugador individualmente
    # El farol recibe es_farol: true, el resto es_farol: false
    for jugador_ws, nombre in salas[codigo]["jugadores"].items():
        respuesta = {
            "tipo":     "rol_asignado",
            "es_farol": nombre == farol,
        }
        await jugador_ws.send(json.dumps(respuesta))

async def jugador_listo(ws, datos):
    codigo = datos["codigo"]
    nombre = datos["nombre"]

    for jugador_ws in salas[codigo]["jugadores"]:
        respuesta = {
            "tipo":   "jugador_listo",
            "nombre": nombre
        }
        await jugador_ws.send(json.dumps(respuesta))



async def voto(ws, datos):
    codigo = datos["datos"]["codigo_sala"]
    votante = datos["datos"]["votante"]
    votado = datos["datos"]["jugador_votado"]

    salas[codigo]["votos"][votante] = votado

    total_jugadores = len(salas[codigo]["jugadores"])
    total_votos = len(salas[codigo]["votos"])

    if total_votos >= total_jugadores:
        conteo = {}
        for v in salas[codigo]["votos"].values():
            if v not in conteo:
                conteo[v] = 0
            conteo[v] += 1

        eliminado = max(conteo, key=lambda x: conteo[x])
        salas[codigo]["vidas"][eliminado] -= 1
        vidas_restantes = salas[codigo]["vidas"][eliminado]

        salas[codigo]["votos"] = {}

        for jugador_ws in salas[codigo]["jugadores"]:
            await jugador_ws.send(json.dumps({
                "tipo": "resultado_voto",
                "jugador_eliminado": eliminado,
                "vidas_restantes": vidas_restantes
            }))

        jugadores_vivos = []
        for nombre, vidas in salas[codigo]["vidas"].items():
            if vidas > 0:
                jugadores_vivos.append(nombre)

        if len(jugadores_vivos) <= 2:
            ganador = max(salas[codigo]["vidas"], key=lambda x: salas[codigo]["vidas"][x])
            for jugador_ws in salas[codigo]["jugadores"]:
                await jugador_ws.send(json.dumps({
                    "tipo": "partida_terminada",
                    "ganador": ganador
                }))
        else:
            coleccion = database.obtener_coleccion(salas[codigo]["coleccion_id"])
            palabra = random.choice(coleccion["palabras"])
            jugadores_nombres = list(salas[codigo]["jugadores"].values())
            farol = random.choice(jugadores_vivos)

            palabras_señuelo = random.sample(coleccion["palabras"], 12)
            if palabra not in palabras_señuelo:
                palabras_señuelo[0] = palabra
            random.shuffle(palabras_señuelo)



            for jugador_ws, nombre in salas[codigo]["jugadores"].items():
                es_farol = nombre == farol
                await jugador_ws.send(json.dumps({
                    "tipo": "partida_iniciada",
                    "es_farol": es_farol,
                    "palabra": palabra,
                    "palabras": palabras_señuelo
                }))
            await asyncio.sleep(DURACION_DIALOGO)


def obtener_palabra(coleccion):
    # coleccion es el documento que nos devolvió obtener_coleccion()
    # simplemente elegimos una palabra aleatoria de su lista
    palabra = random.choice(coleccion["palabras"])
    return palabra

async def partida_terminada(ws, datos):
    codigo         = datos["codigo"]
    ganador        = datos["ganador"]
    duracion       = datos["duracion"]
    coleccion_usada = datos["coleccion_usada"]
    lista_jugadores = datos["jugadores"]  # lista de { perfil_id, nombre, veces_farol, gano }

    # Guardamos la partida en MongoDB
    partida_id = database.guardar_partida(ganador, duracion, coleccion_usada, [j["nombre"] for j in lista_jugadores])

    # Guardamos cada jugador y actualizamos su perfil
    for j in lista_jugadores:
        database.guardar_jugador(partida_id, j["perfil_id"], j["nombre"], j["veces_farol"], j["gano"])
        database.actualizar_perfil(j["perfil_id"], partida_id, j["gano"])

    # Avisamos a todos de que la partida se guardó
    for jugador_ws in salas.salas[codigo]["jugadores"]:
        await jugador_ws.send(json.dumps({"tipo": "partida_guardada", "partida_id": str(partida_id)}))

async def pedir_historial(ws, datos):
    perfil_id = datos["perfil_id"]
    historial = database.obtener_historial(perfil_id)
    await ws.send(json.dumps({"tipo": "historial", "historial": historial}))


async def login(ws, datos):
    usuario = datos["datos"]["emailONombre"]
    password = datos["datos"]["password"]

    perfil_id = database.login(usuario, password)
    if perfil_id:
        await ws.send(json.dumps({"tipo": "login_ok", "perfil_id": perfil_id}))
    else:
        await ws.send(json.dumps({"tipo": "login_error"}))


async def registro(ws, datos):
    nombre = datos["datos"]["nombre"]
    email = datos["datos"]["email"]
    password = datos["datos"]["password"]

    perfil_id = database.registro(nombre, email, password)
    if perfil_id:
        await ws.send(json.dumps({"tipo": "registro_ok", "perfil_id": perfil_id}))
    else:
        await ws.send(json.dumps({"tipo": "registro_error"}))

async def pedir_perfil(ws, datos):
    perfil_id = datos["datos"]["perfil_id"]
    perfil = database.obtener_perfil(perfil_id)
    if perfil:
        await ws.send(json.dumps({
            "tipo": "perfil_ok",
            "nombre": perfil["nombre"],
            "email": perfil["email"],
            "partidas_jugadas": perfil["partidas_jugadas"],
            "partidas_ganadas": perfil["partidas_ganadas"]
        }))
    else:
        await ws.send(json.dumps({"tipo": "perfil_error"}))

async def pedir_colecciones(ws, datos):
    lista = database.obtener_colecciones()
    await ws.send(json.dumps({"tipo": "lista_colecciones", "colecciones": lista}))

async def pedir_lista_jugadores(ws, datos, salas):
    codigo = datos["datos"]["codigo"]
    if codigo not in salas:
        return
    lista = []
    for nombre in salas[codigo]["jugadores"].values():
        vidas = salas[codigo]["vidas"].get(nombre, 3)
        lista.append({"nombre": nombre, "vidas": vidas})
    await ws.send(json.dumps({"tipo": "lista_jugadores", "jugadores": lista}))

async def guardar_partida_jugador(ws, datos):
    perfil_id = datos["datos"]["perfil_id"]
    ganador = datos["datos"]["ganador"]
    coleccion_id = datos["datos"]["coleccion_id"]
    gano = datos["datos"]["gano"]
    nombre = datos["datos"]["nombre"]

    partida_id = database.guardar_partida(ganador, 0, coleccion_id, [perfil_id])
    database.guardar_jugador(partida_id, perfil_id, nombre, 0, gano)
    database.actualizar_perfil(perfil_id, partida_id, gano)

    await ws.send(json.dumps({"tipo": "partida_guardada"}))