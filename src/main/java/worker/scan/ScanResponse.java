package worker.scan;

import models.AlbumData;

import java.util.List;

record ScanResponse(List<AlbumData> albumData, List<ArtworkPayload> artworks) {
}
