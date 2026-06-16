# Reporte de bugs

A medida que se vayan solucionando, se deben borrar de este archivo.

## En el monitor público

### Rellamados

Si un puesto quiere rellamar a un cliente que quedó en el historial, el sistema debería sacarlo del historial para ponerlo en el cuadro de turno actual. Pero, en lugar de eso, simplemente se parpadea al turno actual, por más que el puesto no haya querido rellamar a ese cliente.

### Posible bug al levantar el historial de turnos

En la implementación del método cargarHistorialInicial se crea el historial de llamados DAO pero con el factory de JSON. ¿Y qué pasa si la persistencia se hizo en XML o en texto plano?
