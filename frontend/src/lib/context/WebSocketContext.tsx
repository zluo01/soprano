import { type Client, createClient } from 'graphql-ws';
import {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useState,
} from 'react';

const WebSocketContext = createContext<Client | null>(null);

function createWSClient() {
  return createClient({
    url: `ws://${window.location.host}/graphql`,
    keepAlive: 10_000,
    lazy: true,
    retryAttempts: 5,
    retryWait: async retries => {
      const delay = Math.min(2000 * Math.pow(2, retries), 10000);
      await new Promise(resolve => setTimeout(resolve, delay));
    },
    shouldRetry: () => true,
  });
}

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const [client, setClient] = useState<Client>(() => createWSClient());

  useEffect(() => {
    const abortController = new AbortController();

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        setClient(prevClient => {
          if (prevClient) {
            try {
              prevClient.dispose();
            } catch (e) {
              console.error('Error disposing:', e);
            }
          }
          return createWSClient();
        });
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange, {
      signal: abortController.signal,
    });

    return () => {
      abortController.abort();
    };
  }, []);

  return (
    <WebSocketContext.Provider value={client}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocketClient() {
  const client = useContext(WebSocketContext);
  if (!client) {
    throw new Error('useWebSocketClient must be used within WebSocketProvider');
  }
  return client;
}
