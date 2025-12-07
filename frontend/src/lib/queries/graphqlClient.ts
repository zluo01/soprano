import { createClient } from 'graphql-ws';

let timedOut: any;

export const graphQLWSClient = createClient({
  url: `ws://${window.location.host}/graphql`,
  keepAlive: 10_000, // ping server every 10 seconds
  on: {
    ping: received => {
      if (!received /* sent */) {
        timedOut = setTimeout(() => {
          // a close event `4499: Terminated` is issued to the current WebSocket and an
          // artificial `{ code: 4499, reason: 'Terminated', wasClean: false }` close-event-like
          // object is immediately emitted without waiting for the one coming from `WebSocket.onclose`
          //
          // calling terminate is not considered fatal and a connection retry will occur as expected
          //
          // see: https://github.com/enisdenjo/graphql-ws/discussions/290
          graphQLWSClient.terminate();
        }, 3_000);
      }
    },
    pong: received => {
      if (received) {
        clearTimeout(timedOut);
      }
    },
  },
});
