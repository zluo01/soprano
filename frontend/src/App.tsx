import Album from '@/components/album';
import Albums from '@/components/albums';
import { GeneralTagAlbumsView, GeneralTagView } from '@/components/general';
import Home from '@/components/home';
import Playlist from '@/components/playlist';
import Playlists from '@/components/playlists';
import { GeneralTag } from '@/type';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Layout from 'src/components/layout';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="playlists">
            <Route index element={<Playlists />} />
            <Route path=":playlistName" element={<Playlist />} />
          </Route>
          <Route path="albums">
            <Route index element={<Albums />} />
            <Route path=":id" element={<Album />} />
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
