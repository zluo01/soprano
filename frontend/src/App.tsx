import Albums from '@/components/albums';
import { GeneralTagAlbumsView, GeneralTagView } from '@/components/general';
import RecentlyAdded from '@/components/recent';
import { GeneralTag } from '@/type';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router';
import Layout from 'src/components/layout';

const Album = lazy(() => import('@/components/album'));
const Playlist = lazy(() => import('@/components/playlist'));
const Playlists = lazy(() => import('@/components/playlists'));

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<RecentlyAdded />} />
          <Route path="playlists">
            <Route
              index
              element={
                <Suspense>
                  <Playlists />
                </Suspense>
              }
            />
            <Route
              path=":playlistName"
              element={
                <Suspense>
                  <Playlist />
                </Suspense>
              }
            />
          </Route>
          <Route path="albums">
            <Route index element={<Albums />} />
            <Route
              path=":id"
              element={
                <Suspense>
                  <Album />
                </Suspense>
              }
            />
          </Route>
          <Route path="artists">
            <Route index element={<GeneralTagView tag={GeneralTag.ARTIST} />} />
            <Route
              path=":id/:name"
              element={<GeneralTagAlbumsView tag={GeneralTag.ARTIST} />}
            />
          </Route>
          <Route path="albumArtists">
            <Route
              index
              element={<GeneralTagView tag={GeneralTag.ALBUM_ARTIST} />}
            />
            <Route
              path=":id/:name"
              element={<GeneralTagAlbumsView tag={GeneralTag.ALBUM_ARTIST} />}
            />
          </Route>
          <Route path="genres">
            <Route index element={<GeneralTagView tag={GeneralTag.GENRE} />} />
            <Route
              path=":id/:name"
              element={<GeneralTagAlbumsView tag={GeneralTag.GENRE} />}
            />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
