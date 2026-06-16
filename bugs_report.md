# Reporte de bugs

A medida que se vayan solucionando, se deben borrar de este archivo.

## En el panel del operador

Si se estaba atendiendo a alguien y, al llamar al siguiente, resulta que no hay un siguiente, sino que la fila se encuentra vacía, el botón de "Re-notificar" se desbloquea de todas formas a los 30 segundos; es decir, que se puede renotificar a nadie.

## En el monitor público

### Rellamados

Si un puesto quiere rellamar a un cliente que quedó en el historial, el sistema debería sacarlo del historial para ponerlo en el cuadro de turno actual. Pero, en lugar de eso, simplemente se parpadea al turno actual, por más que el puesto no haya querido rellamar a ese cliente.

### Posible bug al levantar el historial de turnos

En la implementación del método cargarHistorialInicial se crea el historial de llamados DAO pero con el factory de JSON. ¿Y qué pasa si la persistencia se hizo en XML o en texto plano?
