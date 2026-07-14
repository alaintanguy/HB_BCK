import React, { useState, useEffect, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const SERVER = 'http://10.0.0.34:3001';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

const redIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34]
});

function RecenterMap({ lat, lng }) {
  const map = useMap();
  useEffect(() => {
    if (lat && lng) map.panTo([lat, lng]);
  }, [lat, lng, map]);
  return null;
}

function MichelPage() {
  const [tracking, setTracking] = useState(false);
  const [points, setPoints] = useState([]);
  const [center, setCenter] = useState([45.5017, -73.5673]);
  const intervalRef = useRef(null);

  const pollGPS = () => {
    intervalRef.current = setInterval(async () => {
      try {
        const res = await fetch(`${SERVER}/api/gps`);
        const data = await res.json();
        setPoints(data.points);
        if (data.points.length > 0) {
          const last = data.points[data.points.length - 1];
          setCenter([last.lat, last.lng]);
        }
        if (!data.trackingActive) {
          clearInterval(intervalRef.current);
          setTracking(false);
        }
      } catch (e) {
        console.error('Poll error:', e);
      }
    }, 5000);
  };

  const handleStart = async () => {
    await fetch(`${SERVER}/api/start`, { method: 'POST' });
    setTracking(true);
    setPoints([]);
    pollGPS();
  };

  const handleStop = async () => {
    await fetch(`${SERVER}/api/stop`, { method: 'POST' });
    clearInterval(intervalRef.current);
    setTracking(false);
  };

  useEffect(() => {
    return () => clearInterval(intervalRef.current);
  }, []);

  return (
    <div style={{ padding: 10 }}>
      <h2 style={{ textAlign: 'center' }}>📱 Michel - Live Map</h2>
      <div style={{ textAlign: 'center', marginBottom: 10 }}>
        {!tracking ? (
          <button onClick={handleStart}
            style={{ fontSize: 22, padding: '15px 40px', backgroundColor: '#4CAF50',
                     color: 'white', border: 'none', borderRadius: 10 }}>
            ▶️ START
          </button>
        ) : (
          <button onClick={handleStop}
            style={{ fontSize: 22, padding: '15px 40px', backgroundColor: '#f44336',
                     color: 'white', border: 'none', borderRadius: 10 }}>
            ⏹️ STOP
          </button>
        )}
        <span style={{ marginLeft: 15, fontSize: 18 }}>
          📍 {points.length} points
        </span>
      </div>
      <MapContainer center={center} zoom={15}
        style={{ height: '70vh', width: '100%', borderRadius: 10 }}>
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; OpenStreetMap'
        />
        <RecenterMap lat={center[0]} lng={center[1]} />
        {points.length > 0 && (
          <>
            <Polyline positions={points.map(p => [p.lat, p.lng])} color="red" weight={3} />
            <Marker position={[points[0].lat, points[0].lng]}>
              <Popup>Start</Popup>
            </Marker>
            <Marker position={[points[points.length-1].lat, points[points.length-1].lng]} icon={redIcon}>
              <Popup>Latest ({new Date(points[points.length-1].time).toLocaleTimeString()})</Popup>
            </Marker>
          </>
        )}
      </MapContainer>
    </div>
  );
}

function AlainPage() {
  const [status, setStatus] = useState('waiting');
  const [count, setCount] = useState(0);
  const [lastPos, setLastPos] = useState(null);
  const intervalRef = useRef(null);
  const watchRef = useRef(null);

  const sendGPS = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(async (pos) => {
        const point = {
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
          accuracy: Math.round(pos.coords.accuracy)
        };
        try {
          await fetch(`${SERVER}/api/gps`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(point)
          });
          setLastPos(point);
          setCount(c => c + 1);
        } catch (e) {
          console.error('Send error:', e);
        }
      }, (err) => console.error('GPS error:', err),
      { enableHighAccuracy: true, timeout: 30000 });
    }
  };

  useEffect(() => {
    const checkStatus = setInterval(async () => {
      try {
        const res = await fetch(`${SERVER}/api/gps`);
        const data = await res.json();
        if (data.trackingActive && status === 'waiting') {
          setStatus('sending');
          sendGPS();
          intervalRef.current = setInterval(sendGPS, 60000);
        }
        if (!data.trackingActive && status === 'sending') {
          clearInterval(intervalRef.current);
          setStatus('stopped');
        }
      } catch (e) {}
    }, 3000);
    return () => {
      clearInterval(checkStatus);
      clearInterval(intervalRef.current);
    };
  }, [status]);

  return (
    <div style={{ padding: 20, textAlign: 'center' }}>
      <h1>📡 Alain - GPS Sender</h1>
      {status === 'waiting' && (
        <div style={{ fontSize: 22, marginTop: 50 }}>
          ⏳ Waiting for Michel to start...
          <br/><br/>
          <span style={{ fontSize: 14, color: '#666' }}>Keep this page open</span>
        </div>
      )}
      {status === 'sending' && (
        <div style={{ fontSize: 22, marginTop: 30, color: '#4CAF50' }}>
          🟢 Sending GPS every 1 minute
          <br/><br/>
          <span style={{ fontSize: 64, fontWeight: 'bold' }}>{count}</span>
          <br/>points sent
          {lastPos && (
            <p style={{ fontSize: 14, color: '#666', marginTop: 20 }}>
              Last: {lastPos.lat.toFixed(6)}, {lastPos.lng.toFixed(6)} ±{lastPos.accuracy}m
            </p>
          )}
        </div>
      )}
      {status === 'stopped' && (
        <div style={{ fontSize: 22, marginTop: 50, color: '#f44336' }}>
          🔴 Stopped<br/>{count} points sent ✅
        </div>
      )}
    </div>
  );
}

function App() {
  const [role, setRole] = useState(null);
  if (!role) {
    return (
      <div style={{ padding: 40, textAlign: 'center' }}>
        <h1>🛰️ GPS Tracker</h1>
        <h2>Who are you?</h2>
        <br/>
        <button onClick={() => setRole('michel')}
          style={{ fontSize: 26, padding: '20px 50px', margin: 10,
                   backgroundColor: '#2196F3', color: 'white', border: 'none', borderRadius: 15 }}>
          📱 Michel
        </button>
        <br/><br/>
        <button onClick={() => setRole('alain')}
          style={{ fontSize: 26, padding: '20px 50px', margin: 10,
                   backgroundColor: '#FF9800', color: 'white', border: 'none', borderRadius: 15 }}>
          📡 Alain
        </button>
      </div>
    );
  }
  return role === 'michel' ? <MichelPage /> : <AlainPage />;
}

export default App;
