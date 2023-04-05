package generator

import requests.Response


object RestExternalCallUtils {
  def summarizeAbstract(url: String, headers: Map[String, String], data: String, readTimeout: Int, connectionTimeout: Int): Response = {
    val connection = requests.Session(headers = headers, readTimeout = readTimeout, connectTimeout = connectionTimeout)
    val response: Response = connection.post(url = url, data = data)
    response
  }
}
