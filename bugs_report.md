# Reporte de bugs

A medida que se vayan solucionando, se deben borrar de este archivo.

## En el monitor público

### Posible bug al levantar el historial de turnos

En la implementación del método cargarHistorialInicial se crea el historial de llamados DAO pero con el factory de JSON. ¿Y qué pasa si la persistencia se hizo en XML o en texto plano?
