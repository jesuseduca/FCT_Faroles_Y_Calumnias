import json
from pymongo import MongoClient
from bson import ObjectId

# Conectamos a MongoDB local
cliente = MongoClient("mongodb://localhost:27017")

# Seleccionamos la base de datos y las colecciones
db          = cliente["faroles_db"]
colecciones = db["colecciones"]
partidas    = db["partidas"]
jugadores   = db["jugadores"]
perfiles    = db["perfiles"]

def obtener_coleccion(mazo_id):
    # Buscamos la colección por su _id
    # Necesitamos convertir el string a ObjectId porque así lo guarda MongoDB
    coleccion = colecciones.find_one({"_id": ObjectId(mazo_id)})
    return coleccion

def guardar_partida(ganador, duracion, coleccion_usada, lista_jugadores):
    partida = {
        "duracion_partida": duracion,
        "ganador":          ganador,
        "coleccion_usada":  coleccion_usada,
        "lista_jugadores":  lista_jugadores
    }
    resultado = partidas.insert_one(partida)
    # insert_one devuelve el _id del documento insertado, lo guardamos
    # porque lo necesitamos para asociar los jugadores a esta partida
    return resultado.inserted_id

def guardar_jugador(partida_id, perfil_id, nombre, veces_farol, gano):
    jugador = {
        "partida_id":  partida_id,
        "perfil_id":   perfil_id,
        "nombre":      nombre,
        "veces_farol": veces_farol,
        "gano":        gano
    }
    jugadores.insert_one(jugador)

def actualizar_perfil(perfil_id, partida_id, gano):
    perfiles.update_one(
        {"_id": ObjectId(perfil_id)},
        {
            "$inc":  {
                "partidas_jugadas": 1,
                "partidas_ganadas": 1 if gano else 0
            },
            "$push": {"id_partidas": partida_id}
        }
    )

def obtener_historial(perfil_id):
    # Buscamos todos los documentos de jugadores que tengan ese perfil_id
    registros = jugadores.find({"perfil_id": perfil_id})

    historial = []
    for registro in registros:
        # Para cada registro buscamos los datos de su partida
        partida = partidas.find_one({"_id": registro["partida_id"]})
        historial.append({
            "partida_id":       str(registro["partida_id"]),
            "nombre":           registro["nombre"],
            "veces_farol":      registro["veces_farol"],
            "gano":             registro["gano"],
            "ganador":          partida["ganador"],
            "coleccion_usada":  partida["coleccion_usada"],
            "duracion_partida": partida["duracion_partida"]
        })

    return historial


def login(usuario, password):
    # Buscamos por email o por nombre
    perfil = perfiles.find_one({
        "$or": [
            {"email":  usuario},
            {"nombre": usuario}
        ],
        "password": password
    })
    if perfil:
        return str(perfil["_id"])
    return None


def registro(nombre, email, password):
    # Comprobamos que no exista ya ese email
    if perfiles.find_one({"email": email}):
        return None

    perfil = {
        "nombre": nombre,
        "email": email,
        "password": password,
        "partidas_jugadas": 0,
        "partidas_ganadas": 0,
        "id_partidas": []
    }
    resultado = perfiles.insert_one(perfil)
    return str(resultado.inserted_id)

def obtener_perfil(perfil_id):
    perfil = perfiles.find_one({"_id": ObjectId(perfil_id)})
    if perfil:
        return {
            "nombre":           perfil["nombre"],
            "email":            perfil["email"],
            "partidas_jugadas": perfil["partidas_jugadas"],
            "partidas_ganadas": perfil["partidas_ganadas"]
        }
    return None
def obtener_colecciones():
    resultado = colecciones.find({}, {"_id": 1, "nombre": 1})
    lista = []
    for c in resultado:
        lista.append({
            "id": str(c["_id"]),
            "nombre": c["nombre"]
        })
    return lista