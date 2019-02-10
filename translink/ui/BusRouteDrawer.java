package ca.ubc.cs.cpsc210.translink.ui;

import android.content.Context;
import android.hardware.camera2.params.MeteringRectangle;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.model.*;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ca.ubc.cs.cpsc210.translink.util.Geometry.gpFromLatLon;
import static ca.ubc.cs.cpsc210.translink.util.Geometry.rectangleContainsPoint;
import static ca.ubc.cs.cpsc210.translink.util.Geometry.rectangleIntersectsLine;

// A bus route drawer
public class BusRouteDrawer extends MapViewOverlay {
    /**
     * overlay used to display bus route legend text on a layer above the map
     */
    private BusRouteLegendOverlay busRouteLegendOverlay;
    /**
     * overlays used to plot bus routes
     */
    private List<Polyline> busRouteOverlays;

    /**
     * Constructor
     *
     * @param context the application context
     * @param mapView the map view
     */
    public BusRouteDrawer(Context context, MapView mapView) {
        super(context, mapView);
        busRouteLegendOverlay = createBusRouteLegendOverlay();
        busRouteOverlays = new ArrayList<>();
    }

    /**
     * Plot each visible segment of each route pattern of each route going through the selected stop.
     */
    public void plotRoutes(int zoomLevel) {
        if (!(StopManager.getInstance().getSelected() == null)) {
            busRouteOverlays.clear();
            busRouteLegendOverlay.clear();
            for (Route route: StopManager.getInstance().getSelected().getRoutes()) {
                busRouteLegendOverlay.add(route.getNumber());
                preSetPolyline(route,zoomLevel);
            }
        }
    }

    private void preSetPolyline(Route route, int zoomlevel) {
        int color = busRouteLegendOverlay.getColor(route.getNumber());
        for (RoutePattern routePattern: route.getPatterns()) {
            setPolyline(routePattern.getPath(),color,zoomlevel);
        }
    }

    private void setPolyline(List<LatLon> path, int color, int zoomlevel) {
        updateVisibleArea();
        List<LatLon> llList = new ArrayList<>();
        List<GeoPoint> gpList = new ArrayList<>();
        Polyline polyline;

        for (LatLon ll: path) {
            checkMethod(color, zoomlevel, llList, gpList, ll);
        }
        polyline = new Polyline(context);
        setPolylineAttributes(llList,gpList,polyline,color,zoomlevel);
    }

    private void checkMethod(int color, int zoomlevel, List<LatLon> llList, List<GeoPoint> gpList, LatLon ll) {
        Polyline polyline;
        if (llList.size() <= 1) {
            llList.add(ll);
        } else if (!(rectangleContainsPoint(northWest, southEast, llList.get(llList.size() - 2)))
                && !(rectangleContainsPoint(northWest,southEast,llList.get(llList.size() - 1)))) {
            checkMethodSecondCondition(llList, ll);
        } else if (rectangleIntersectsLine(northWest,southEast,llList.get(llList.size() - 2),
                llList.get(llList.size() - 1))) {
            polyline = new Polyline(context);
            windowDoesNotContainLastElement(color, zoomlevel, llList, gpList, polyline);
            llList.add(ll);
        } else {
            llList.add(ll);
        }
    }

    private void windowDoesNotContainLastElement(int color, int zoomlevel, List<LatLon> llList,
                                                 List<GeoPoint> gpList, Polyline polyline) {
        if (!rectangleContainsPoint(northWest, southEast, llList.get(llList.size() - 1))) {
            LatLon l = llList.get(llList.size() - 1);
            setPolylineAttributes(llList,gpList,polyline,color,zoomlevel);
            llList.clear();
            llList.add(l);
            gpList.clear();
        }
    }

    private void checkMethodSecondCondition(List<LatLon> llList, LatLon ll) {
        if (llList.size() == 2) {
            llList.remove(llList.get(llList.size() - 2));
            llList.add(ll);
        } else {
            llList.remove(llList.get(llList.size() - 1));
            llList.add(ll);
        }
    }


    private void setPolylineAttributes(List<LatLon> llList, List<GeoPoint> gpList,
                                       Polyline polyline, int color, int zoomlevel) {
        for (LatLon ll: llList) {
            gpList.add(gpFromLatLon(ll));
        }
        polyline.setPoints(gpList);
        polyline.setColor(color);
        polyline.setWidth(getLineWidth(zoomlevel));
        polyline.setPoints(gpList);
        polyline.setVisible(true);
        busRouteOverlays.add(polyline);
    }


    public List<Polyline> getBusRouteOverlays() {
        return Collections.unmodifiableList(busRouteOverlays);
    }

    public BusRouteLegendOverlay getBusRouteLegendOverlay() {
        return busRouteLegendOverlay;
    }


    /**
     * Create text overlay to display bus route colours
     */
    private BusRouteLegendOverlay createBusRouteLegendOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(context);
        return new BusRouteLegendOverlay(rp, BusesAreUs.dpiFactor());
    }

    /**
     * Get width of line used to plot bus route based on zoom level
     *
     * @param zoomLevel the zoom level of the map
     * @return width of line used to plot bus route
     */
    private float getLineWidth(int zoomLevel) {
        if (zoomLevel > 14) {
            return 7.0f * BusesAreUs.dpiFactor();
        } else if (zoomLevel > 10) {
            return 5.0f * BusesAreUs.dpiFactor();
        } else {
            return 2.0f * BusesAreUs.dpiFactor();
        }
    }
}
