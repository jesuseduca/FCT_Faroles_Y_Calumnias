import random
import json
from salas import salas
import database

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
    codigo  = datos["codigo"]
    votante = datos["votante"]
    votado  = datos["votado"]

    for jugador_ws in salas[codigo]["jugadores"]:
        respuesta = {
            "tipo":    "voto",
            "votante": votante,
            "votado":  votado
        }
        await jugador_ws.send(json.dumps(respuesta))


async def resultado_adivinanza(ws, datos):
    codigo = datos["codigo"]
    acerto = datos["acerto"]

    for jugador_ws in salas[codigo]["jugadores"]:
        respuesta = {
            "tipo":   "resultado_adivinanza",
            "acerto": acerto
        }
        await jugador_ws.send(json.dumps(respuesta))

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
    usuario = datos["usuario"]
    password = datos["password"]

    perfil_id = database.login(usuario, password)
    if perfil_id:
        await ws.send(json.dumps({"tipo": "login_ok", "perfil_id": perfil_id}))
    else:
        await ws.send(json.dumps({"tipo": "error", "mensaje": "Usuario o contraseña incorrectos"}))


async def registro(ws, datos):
    nombre = datos["nombre"]
    email = datos["email"]
    password = datos["password"]

    perfil_id = database.registro(nombre, email, password)
    if perfil_id:
        await ws.send(json.dumps({"tipo": "registro_ok", "perfil_id": perfil_id}))
    else:
        await ws.send(json.dumps({"tipo": "error", "mensaje": "Ese email ya está registrado"}))