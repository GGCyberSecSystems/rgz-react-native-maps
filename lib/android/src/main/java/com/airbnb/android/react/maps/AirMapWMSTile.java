package com.airbnb.android.react.maps;

import android.content.Context;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;

import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.ProjCoordinate;

import java.net.MalformedURLException;
import java.net.URL;

public class AirMapWMSTile extends AirMapUrlTile {
  private static final double[] mapBound = {-20037508.34789244, 20037508.34789244};
  private static final double FULL = 20037508.34789244 * 2;

  class AIRMapGSUrlTileProvider extends AirMapTileProvider {

    class AIRMapWMSTileProvider extends UrlTileProvider {
    private String urlTemplate;
    private int tileSize;

    public AIRMapWMSTileProvider(int width, int height, String urlTemplate) {
      super(width, height);
      this.urlTemplate = urlTemplate;
      this.tileSize = width;
    }

    @Override
    public URL getTileUrl(int x, int y, int zoom) {
      if(AirMapWMSTile.this.maximumZ > 0 && zoom > maximumZ) {
          return null;
      }

      if(AirMapWMSTile.this.minimumZ > 0 && zoom < minimumZ) {
          return null;
      }

      CRSFactory factory = new CRSFactory();
      CoordinateReferenceSystem srcCrs = factory.createFromName("EPSG:3857");
      CoordinateReferenceSystem dstCrs = factory.createFromName("EPSG:32634");

      BasicCoordinateTransform transform = new BasicCoordinateTransform(srcCrs, dstCrs);

      double[] bb = getBoundingBox(x, y, zoom);

      ProjCoordinate srcCoordMIN = new ProjCoordinate(bb[0], bb[1]);
      ProjCoordinate dstCoorMin = new ProjCoordinate();
      transform.transform(srcCoordMIN, dstCoorMin);

      ProjCoordinate srcCoordMax = new ProjCoordinate(bb[2], bb[3]);
      ProjCoordinate dstCoorMax = new ProjCoordinate();
      transform.transform(srcCoordMax, dstCoorMax);

      String s = this.urlTemplate
          .replace("{minX}", Double.toString(dstCoorMin.x))
          .replace("{minY}", Double.toString(dstCoorMin.y))
          .replace("{maxX}", Double.toString(dstCoorMax.x))
          .replace("{maxY}", Double.toString(dstCoorMax.y))
          .replace("{width}", Integer.toString(this.tileSize))
          .replace("{height}", Integer.toString(this.tileSize));
      URL url = null;
      Log.i("MAPA", s);

      try {
        url = new URL(s);
      } catch (MalformedURLException e) {
        throw new AssertionError(e);
      }
      return url;
    }

    private double[] getBoundingBox(int x, int y, int zoom) {
      double tile = FULL / Math.pow(2, zoom);
      return new double[]{
              mapBound[0] + x * tile,
              mapBound[1] - (y + 1) * tile,
              mapBound[0] + (x + 1) * tile,
              mapBound[1] - y * tile
      };
    }

    public void setUrlTemplate(String urlTemplate) {
      this.urlTemplate = urlTemplate;
    }
  }

  public AIRMapGSUrlTileProvider(int tileSizet, String urlTemplate, 
    int maximumZ, int maximumNativeZ, int minimumZ, String tileCachePath, 
    int tileCacheMaxAge, boolean offlineMode, Context context, boolean customMode) {
      super(tileSizet, false, urlTemplate, maximumZ, maximumNativeZ, minimumZ, false,
        tileCachePath, tileCacheMaxAge, offlineMode, context, customMode);
      this.tileProvider = new AIRMapWMSTileProvider(tileSizet, tileSizet, urlTemplate);
    }
  }

  private AIRMapGSUrlTileProvider tileProvider;

  public AirMapWMSTile(Context context) {
    super(context);
  }

  @Override
  protected TileOverlayOptions createTileOverlayOptions() {
    TileOverlayOptions options = new TileOverlayOptions();
    options.zIndex(zIndex);
    options.transparency(1 - this.opacity);
    this.tileProvider = new AIRMapGSUrlTileProvider((int)this.tileSize, this.urlTemplate, 
      (int)this.maximumZ, (int)this.maximumNativeZ, (int)this.minimumZ, this.tileCachePath, 
      (int)this.tileCacheMaxAge, this.offlineMode, this.context, this.customTileProviderNeeded);
    options.tileProvider(this.tileProvider);
    return options;
  }
}
