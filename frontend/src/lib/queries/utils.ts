import { QueryClient } from '@tanstack/react-query';

const BASE_URL =
  import.meta.env.MODE === 'development' ? 'http://localhost:6868' : '';

export const QUERY_CLIENT = new QueryClient();

export const IMMUTABLE_REQUEST = {
  staleTime: Infinity,
  refetchOnWindowFocus: false,
  refetchOnReconnect: false,
};

export async function request<T>(
  query: string,
  variables: object = {},
): Promise<T> {
  const response = await fetch(BASE_URL + '/graphql', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/graphql-response+json',
    },
    body: JSON.stringify({
      query,
      variables,
    }),
  });

  if (!response.ok) {
    throw new Error('Network response was not ok.' + response.statusText);
  }

  return (await response.json()).data as T;
}

export function constructImg(
  width: number,
  height: number,
  albumId?: number,
): string {
  return BASE_URL + `/covers/${albumId}_${width}x${height}.webp`;
}
