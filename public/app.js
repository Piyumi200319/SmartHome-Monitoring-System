/* ==========================================================================
   SmartHome Web-Based Hardware Simulator - SCS 3311
   Real-Time Bidirectional Synchronization & Hardware Engine
   ========================================================================== */

// --------------------------------------------------------------------------
// 1. Firebase Firestore Configuration & Initialization
// --------------------------------------------------------------------------
const firebaseConfig = {
  apiKey: "AIzaSyBd0Dp0vjc4-1bVjOTpEDJbCqRQ6w8deq0",
  authDomain: "smarthome-scs3311-92dfe.firebaseapp.com",
  projectId: "smarthome-scs3311-92dfe",
  storageBucket: "smarthome-scs3311-92dfe.firebasestorage.app",
  messagingSenderId: "669407847164",
  appId: "1:669407847164:android:d5556daa40483f85946d79"
};

// Initialize Firebase
if (!firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}
const db = firebase.firestore();

// Enable Experimental Force Long Polling for ultra-fast local network / proxy real-time snapshot delivery
try {
  db.settings({ experimentalForceLongPolling: true });
} catch (e) {
  console.log("Firestore settings note:", e.message);
}

// --------------------------------------------------------------------------
// 2. Application State & Storage
// --------------------------------------------------------------------------
let floors = [];
let rooms = [];
let devices = [];

let selectedFloorId = "ALL";
let selectedRoomId = "ALL";
let searchQuery = "";

let activeModalDeviceId = null;
let ironLocalTimers = {}; // Local interval timers for iron countdowns
let scheduleInterval = null;
let cameraClockInterval = null;

// --------------------------------------------------------------------------
// 3. DOM Elements
// --------------------------------------------------------------------------
const syncStatusText = document.getElementById("syncStatusText");
const firebaseSyncStatus = document.getElementById("firebaseSyncStatus");

const statTotalDevices = document.getElementById("statTotalDevices");
const statActiveDevices = document.getElementById("statActiveDevices");
const statTotalPower = document.getElementById("statTotalPower");
const statTotalEnergy = document.getElementById("statTotalEnergy");

const floorTabsContainer = document.getElementById("floorTabs");
const roomTabsContainer = document.getElementById("roomTabs");
const searchInput = document.getElementById("searchInput");
const deviceGrid = document.getElementById("deviceGrid");

// Modal Elements
const deviceModal = document.getElementById("deviceModal");
const modalCloseBtn = document.getElementById("modalCloseBtn");
const modalDeviceIcon = document.getElementById("modalDeviceIcon");
const modalDeviceTitle = document.getElementById("modalDeviceTitle");
const modalDeviceSub = document.getElementById("modalDeviceSub");

const alertError = document.getElementById("alertError");
const alertDisconnected = document.getElementById("alertDisconnected");

// Control Panels
const panelOutlet = document.getElementById("panelOutlet");
const panelLight = document.getElementById("panelLight");
const panelSwitch = document.getElementById("panelSwitch");
const panelIron = document.getElementById("panelIron");
const panelCamera = document.getElementById("panelCamera");

// Electrical Telemetry Elements
const lblDevicePower = document.getElementById("lblDevicePower");
const lblDeviceVoltage = document.getElementById("lblDeviceVoltage");
const lblDeviceCurrent = document.getElementById("lblDeviceCurrent");
const lblDeviceEnergy = document.getElementById("lblDeviceEnergy");

// Schedule Elements
const lblScheduleStatus = document.getElementById("lblScheduleStatus");
const btnScheduleOn = document.getElementById("btnScheduleOn");
const btnScheduleOff = document.getElementById("btnScheduleOff");
const btnScheduleCancel = document.getElementById("btnScheduleCancel");

// --------------------------------------------------------------------------
// 4. Initialize Real-Time Snapshot Listeners
// --------------------------------------------------------------------------
function initFirebaseListeners() {
  console.log("Connecting to Firestore snapshot listeners...");

  // A. Listen to Floors (With Fallback if orderBy fails)
  function attachFloorsListener(useOrderBy = true) {
    let query = useOrderBy ? db.collection("floors").orderBy("order") : db.collection("floors");
    query.onSnapshot(snapshot => {
      floors = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      console.log(`Floors loaded (${floors.length})`);
      renderFloorTabs();
      renderDeviceGrid();
    }, err => {
      console.warn("Floors snapshot warning, trying unordered fallback:", err.message);
      if (useOrderBy) attachFloorsListener(false);
    });
  }
  attachFloorsListener(true);

  // B. Listen to Rooms
  db.collection("rooms").onSnapshot(snapshot => {
    rooms = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    console.log(`Rooms loaded (${rooms.length})`);
    renderRoomTabs();
    renderDeviceGrid();
  }, err => {
    console.error("Rooms snapshot error:", err.message);
  });

  // C. Listen to Devices (Core Real-Time Engine)
  db.collection("devices").onSnapshot(snapshot => {
    devices = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    console.log(`Devices updated live (${devices.length} hardware units)`);
    
    // Immediately update header status badge
    syncStatusText.textContent = `Real-Time Sync Active (${devices.length} Devices)`;
    firebaseSyncStatus.style.borderColor = "rgba(34, 197, 94, 0.4)";
    firebaseSyncStatus.style.background = "rgba(34, 197, 94, 0.15)";

    updateGlobalStats();
    renderDeviceGrid();
    
    // If modal detail is currently open, refresh modal view live!
    if (activeModalDeviceId) {
      const activeDev = devices.find(d => d.id === activeModalDeviceId);
      if (activeDev) {
        updateModalContent(activeDev);
      }
    }

    // Process Iron Timers & Scheduled Actions
    processIronTimers();
  }, err => {
    console.error("Devices snapshot error:", err);
    syncStatusText.textContent = "Sync Error (Devices)";
    firebaseSyncStatus.style.borderColor = "rgba(239, 68, 68, 0.4)";
  });
}

// --------------------------------------------------------------------------
// 5. Global Telemetry Dashboard Updater
// --------------------------------------------------------------------------
function updateGlobalStats() {
  const total = devices.length;
  let activeCount = 0;
  let totalPower = 0;
  let totalEnergy = 0;

  devices.forEach(dev => {
    const isDeviceOn = isDeviceActive(dev);
    if (isDeviceOn) {
      activeCount++;
    }
    totalPower += Number(dev.power || 0);
    totalEnergy += Number(dev.energyToday || 0);
  });

  statTotalDevices.textContent = total;
  statActiveDevices.textContent = activeCount;
  statTotalPower.textContent = `${totalPower} W`;
  statTotalEnergy.textContent = `${totalEnergy.toFixed(2)} kWh`;
}

function isDeviceActive(device) {
  const status = (device.status || "").trim().toUpperCase();
  if (status === "ERROR" || status === "DISCONNECTED") return false;
  return status === "ON" || device.isOn === true;
}

// --------------------------------------------------------------------------
// 6. Navigation Tabs & Filtering
// --------------------------------------------------------------------------
function renderFloorTabs() {
  floorTabsContainer.innerHTML = `
    <button class="tab-btn ${selectedFloorId === 'ALL' ? 'active' : ''}" data-floor-id="ALL">
      <span>🏢</span> All Floors
    </button>
  `;

  floors.forEach(floor => {
    const floorName = floor.floorName || floor.name || "Floor";
    const icon = floor.floorIcon || floor.icon || floor.emoji || "🏢";
    const btn = document.createElement("button");
    btn.className = `tab-btn ${selectedFloorId === floor.id ? 'active' : ''}`;
    btn.dataset.floorId = floor.id;
    btn.innerHTML = `<span>${icon}</span> ${floorName}`;
    floorTabsContainer.appendChild(btn);
  });
}

function renderRoomTabs() {
  roomTabsContainer.innerHTML = `
    <button class="tab-btn ${selectedRoomId === 'ALL' ? 'active' : ''}" data-room-id="ALL">
      <span>🚪</span> All Rooms
    </button>
  `;

  // Filter rooms by selected floor if specified
  const filteredRooms = (selectedFloorId === 'ALL')
    ? rooms
    : rooms.filter(r => r.floorId === selectedFloorId);

  filteredRooms.forEach(room => {
    const roomName = room.roomName || room.name || "Room";
    const icon = room.roomIcon || room.icon || room.emoji || "🚪";
    const btn = document.createElement("button");
    btn.className = `tab-btn ${selectedRoomId === room.id ? 'active' : ''}`;
    btn.dataset.roomId = room.id;
    btn.innerHTML = `<span>${icon}</span> ${roomName}`;
    roomTabsContainer.appendChild(btn);
  });
}

// Event Listeners for Filters
floorTabsContainer.addEventListener("click", e => {
  const btn = e.target.closest(".tab-btn");
  if (!btn) return;
  selectedFloorId = btn.dataset.floorId;
  selectedRoomId = "ALL"; // Reset room selection when floor changes
  renderFloorTabs();
  renderRoomTabs();
  renderDeviceGrid();
});

roomTabsContainer.addEventListener("click", e => {
  const btn = e.target.closest(".tab-btn");
  if (!btn) return;
  selectedRoomId = btn.dataset.roomId;
  renderRoomTabs();
  renderDeviceGrid();
});

searchInput.addEventListener("input", e => {
  searchQuery = e.target.value.toLowerCase().trim();
  renderDeviceGrid();
});

// --------------------------------------------------------------------------
// 7. Render Device Grid & Cards
// --------------------------------------------------------------------------
function renderDeviceGrid() {
  let filtered = devices.filter(dev => {
    // Floor filter
    if (selectedFloorId !== "ALL") {
      const room = rooms.find(r => r.id === dev.roomId);
      if (!room || room.floorId !== selectedFloorId) return false;
    }
    // Room filter
    if (selectedRoomId !== "ALL" && dev.roomId !== selectedRoomId) {
      return false;
    }
    // Search query filter
    if (searchQuery) {
      const nameMatch = (dev.name || "").toLowerCase().includes(searchQuery);
      const typeMatch = (dev.type || "").toLowerCase().includes(searchQuery);
      const room = rooms.find(r => r.id === dev.roomId);
      const roomMatch = room && (room.roomName || room.name || "").toLowerCase().includes(searchQuery);
      if (!nameMatch && !typeMatch && !roomMatch) return false;
    }
    return true;
  });

  if (filtered.length === 0) {
    if (devices.length === 0) {
      deviceGrid.innerHTML = `
        <div class="empty-state">
          <h3>Connecting to Smart Home Database...</h3>
          <p>Fetching real-time hardware status from Firestore.</p>
        </div>
      `;
    } else {
      deviceGrid.innerHTML = `
        <div class="empty-state">
          <h3>No Hardware Devices Match Filter</h3>
          <p>Try clearing your search query or selecting "All Floors".</p>
        </div>
      `;
    }
    return;
  }

  deviceGrid.innerHTML = "";
  filtered.forEach(device => {
    const card = createDeviceCard(device);
    deviceGrid.appendChild(card);
  });
}

function getDeviceTypeIcon(type) {
  switch ((type || "").trim().toLowerCase()) {
    case "iron": return "🔥";
    case "light": return "💡";
    case "camera": return "📹";
    case "switch": return "🎛️";
    case "outlet": return "🔌";
    default: return "⚡";
  }
}

function getDeviceLocationText(roomId) {
  const room = rooms.find(r => r.id === roomId);
  if (!room) return "Smart Home";
  const roomName = room.roomName || room.name || "Room";
  const floor = floors.find(f => f.id === room.floorId);
  const floorName = floor ? (floor.floorName || floor.name || "Floor") : "";
  return floorName ? `${floorName} • ${roomName}` : roomName;
}

function createDeviceCard(device) {
  const card = document.createElement("div");
  card.className = "device-card";
  
  const status = (device.status || "OFF").trim().toUpperCase();
  const isOn = isDeviceActive(device);
  card.dataset.status = status;

  const icon = getDeviceTypeIcon(device.type);
  const locationText = getDeviceLocationText(device.roomId);

  let summaryHTML = "";
  const devType = (device.type || "").toLowerCase();

  if (devType === "light") {
    summaryHTML = `
      <div class="summary-item"><div class="label">Brightness</div><div class="value">${device.brightness || 80}%</div></div>
      <div class="summary-item"><div class="label">Power</div><div class="value">${device.power || 0} W</div></div>
    `;
  } else if (devType === "iron") {
    summaryHTML = `
      <div class="summary-item"><div class="label">Timer Remaining</div><div class="value">${device.timer || 0} s</div></div>
      <div class="summary-item"><div class="label">Safety Mode</div><div class="value">${device.safetyMode || 'SAFE'}</div></div>
    `;
  } else if (devType === "camera") {
    summaryHTML = `
      <div class="summary-item"><div class="label">Stream Mode</div><div class="value">${isOn ? 'LIVE' : 'OFFLINE'}</div></div>
      <div class="summary-item"><div class="label">Night Vision</div><div class="value">${device.nightVision ? 'ENABLED' : 'OFF'}</div></div>
    `;
  } else if (devType === "switch") {
    summaryHTML = `
      <div class="summary-item"><div class="label">Switches (1/2/3)</div><div class="value">${device.switch1 ? 'ON' : 'OFF'} | ${device.switch2 ? 'ON' : 'OFF'} | ${device.switch3 ? 'ON' : 'OFF'}</div></div>
      <div class="summary-item"><div class="label">Power</div><div class="value">${device.power || 0} W</div></div>
    `;
  } else {
    // Outlet
    summaryHTML = `
      <div class="summary-item"><div class="label">Load Power</div><div class="value">${device.power || 0} W</div></div>
      <div class="summary-item"><div class="label">Current</div><div class="value">${(device.current || 0).toFixed(2)} A</div></div>
    `;
  }

  card.innerHTML = `
    <div class="card-top">
      <div class="device-header-info">
        <div class="device-type-icon">${icon}</div>
        <div class="device-titles">
          <h3>${device.name || 'Smart Device'}</h3>
          <div class="device-location">${locationText}</div>
        </div>
      </div>
      <span class="status-badge ${status.toLowerCase()}">${status}</span>
    </div>

    <div class="card-summary">
      ${summaryHTML}
    </div>

    <div class="card-footer">
      <button class="btn-open-detail" data-device-id="${device.id}">
        <span>⚙️</span> Open Device Controls & Details
      </button>
    </div>
  `;

  // Attach button click to open modal
  card.querySelector(".btn-open-detail").addEventListener("click", () => {
    openDeviceModal(device.id);
  });

  return card;
}

// --------------------------------------------------------------------------
// 8. Device Detail Modal Engine
// --------------------------------------------------------------------------
function openDeviceModal(deviceId) {
  activeModalDeviceId = deviceId;
  const device = devices.find(d => d.id === deviceId);
  if (!device) return;

  updateModalContent(device);
  deviceModal.classList.add("active");
}

function closeModal() {
  deviceModal.classList.remove("active");
  activeModalDeviceId = null;
}

modalCloseBtn.addEventListener("click", closeModal);
deviceModal.addEventListener("click", e => {
  if (e.target === deviceModal) closeModal();
});

function updateModalContent(device) {
  modalDeviceIcon.textContent = getDeviceTypeIcon(device.type);
  modalDeviceTitle.textContent = device.name || "Device Detail";
  modalDeviceSub.textContent = getDeviceLocationText(device.roomId);

  const status = (device.status || "OFF").trim().toUpperCase();
  const isOn = isDeviceActive(device);

  // Alert Banners
  alertError.style.display = (status === "ERROR") ? "flex" : "none";
  alertDisconnected.style.display = (status === "DISCONNECTED") ? "flex" : "none";

  // Telemetry Labels
  lblDevicePower.textContent = `${device.power || 0} W`;
  lblDeviceVoltage.textContent = `${device.voltage || 230} V`;
  lblDeviceCurrent.textContent = `${(device.current || 0).toFixed(3)} A`;
  lblDeviceEnergy.textContent = `${(device.energyToday || 0).toFixed(2)} kWh`;

  // Hide all device panels first
  panelOutlet.style.display = "none";
  panelLight.style.display = "none";
  panelSwitch.style.display = "none";
  panelIron.style.display = "none";
  panelCamera.style.display = "none";

  const typeLower = (device.type || "").toLowerCase();

  // Populate Device Specific Panel
  if (typeLower === "outlet") {
    panelOutlet.style.display = "block";
    const toggle = document.getElementById("toggleOutletPower");
    toggle.checked = isOn;
    toggle.onclick = () => {
      const newOn = toggle.checked;
      const updates = {
        isOn: newOn,
        status: newOn ? "ON" : "OFF",
        power: newOn ? 250 : 0,
        current: newOn ? (250 / (device.voltage || 230)) : 0.0
      };
      updateDeviceInFirestore(device.id, updates);
    };
  } else if (typeLower === "light") {
    panelLight.style.display = "block";
    const toggle = document.getElementById("toggleLightPower");
    const slider = document.getElementById("sliderLightBrightness");
    const lblBrightness = document.getElementById("lblLightBrightness");

    toggle.checked = isOn;
    slider.value = device.brightness || 80;
    slider.disabled = !isOn;
    lblBrightness.textContent = `Brightness : ${device.brightness || 80}%`;

    toggle.onclick = () => {
      const newOn = toggle.checked;
      const b = device.brightness || 80;
      const p = newOn ? Math.round((b * 15) / 100) : 0;
      const c = (device.voltage > 0 && newOn) ? p / device.voltage : 0;
      updateDeviceInFirestore(device.id, {
        isOn: newOn,
        status: newOn ? "ON" : "OFF",
        power: p,
        current: c
      });
    };

    slider.oninput = () => {
      lblBrightness.textContent = `Brightness : ${slider.value}%`;
    };

    slider.onchange = () => {
      if (!isOn) return;
      const b = parseInt(slider.value);
      const p = Math.round((b * 15) / 100);
      const c = (device.voltage > 0) ? p / device.voltage : 0;
      updateDeviceInFirestore(device.id, {
        brightness: b,
        power: p,
        current: c
      });
    };
  } else if (typeLower === "switch") {
    panelSwitch.style.display = "block";
    const sw1 = document.getElementById("toggleSwitch1");
    const sw2 = document.getElementById("toggleSwitch2");
    const sw3 = document.getElementById("toggleSwitch3");

    sw1.checked = !!device.switch1;
    sw2.checked = !!device.switch2;
    sw3.checked = !!device.switch3;

    function handleSwitchChange() {
      const s1 = sw1.checked;
      const s2 = sw2.checked;
      const s3 = sw3.checked;
      const newOn = s1 || s2 || s3;
      let power = 0;
      if (s1) power += 1;
      if (s2) power += 1;
      if (s3) power += 1;
      const current = (device.voltage > 0) ? power / device.voltage : 0;

      updateDeviceInFirestore(device.id, {
        switch1: s1,
        switch2: s2,
        switch3: s3,
        isOn: newOn,
        status: newOn ? "ON" : "OFF",
        power: power,
        current: current
      });
    }

    sw1.onclick = handleSwitchChange;
    sw2.onclick = handleSwitchChange;
    sw3.onclick = handleSwitchChange;
  } else if (typeLower === "iron") {
    panelIron.style.display = "block";
    const toggle = document.getElementById("toggleIronPower");
    const lblMaxTime = document.getElementById("lblIronMaxTime");
    const lblTimer = document.getElementById("lblIronTimer");
    const lblSafetyMode = document.getElementById("lblIronSafetyMode");
    const lblHeating = document.getElementById("lblIronHeating");
    const sliderTemp = document.getElementById("sliderIronTemperature");
    const lblTemp = document.getElementById("lblIronTemperature");

    toggle.checked = isOn;
    lblMaxTime.textContent = `${device.maxTime || 120} Minutes`;
    lblTimer.textContent = `${device.timer || 0} s`;
    lblSafetyMode.textContent = device.safetyMode || "SAFE";
    lblHeating.textContent = (isOn && device.heating !== false) ? "HEATING" : "OFF";
    
    sliderTemp.value = device.temperature || 120;
    sliderTemp.disabled = !isOn;
    lblTemp.textContent = `${device.temperature || 120}°C`;

    toggle.onclick = () => {
      const newOn = toggle.checked;
      if (newOn) {
        const maxT = device.maxTime || 120;
        const temp = device.temperature || 120;
        const p = calculateIronPower(temp);
        const c = (device.voltage > 0) ? p / device.voltage : 0;
        updateDeviceInFirestore(device.id, {
          isOn: true,
          status: "ON",
          timer: maxT,
          heating: true,
          safetyMode: "SAFE",
          power: p,
          current: c
        });
      } else {
        updateDeviceInFirestore(device.id, {
          isOn: false,
          status: "OFF",
          timer: 0,
          heating: false,
          power: 0,
          current: 0,
          safetyMode: "SAFE"
        });
      }
    };

    sliderTemp.oninput = () => {
      lblTemp.textContent = `${sliderTemp.value}°C`;
    };

    sliderTemp.onchange = () => {
      if (!isOn) return;
      const t = parseInt(sliderTemp.value);
      const p = calculateIronPower(t);
      const c = (device.voltage > 0) ? p / device.voltage : 0;
      updateDeviceInFirestore(device.id, {
        temperature: t,
        power: p,
        current: c
      });
    };
  } else if (typeLower === "camera") {
    panelCamera.style.display = "block";
    const togglePower = document.getElementById("toggleCameraPower");
    const toggleRec = document.getElementById("toggleCameraRecording");
    const toggleMotion = document.getElementById("toggleCameraMotion");
    const toggleNight = document.getElementById("toggleCameraNightVision");

    const lblLiveStatus = document.getElementById("lblCameraLiveStatus");
    const dotRec = document.getElementById("dotRec");
    const tintNight = document.getElementById("tintNightVision");
    const boxMotion = document.getElementById("boxMotion");

    document.getElementById("lblCameraResolution").textContent = device.resolution || "1080P";
    document.getElementById("lblCameraFPS").textContent = `${device.fps || 30} FPS`;

    togglePower.checked = isOn;
    toggleRec.checked = !!device.recording;
    toggleMotion.checked = (device.motionDetection !== false);
    toggleNight.checked = !!device.nightVision;

    lblLiveStatus.textContent = status === "ON" || (status !== "ERROR" && status !== "DISCONNECTED" && isOn) ? "LIVE" : status;
    dotRec.style.display = device.recording ? "inline-block" : "none";
    tintNight.className = `night-vision-tint ${device.nightVision ? 'active' : ''}`;
    boxMotion.className = `motion-box ${(device.motionDetection !== false) ? 'active' : ''}`;

    togglePower.onclick = () => {
      const newOn = togglePower.checked;
      updateDeviceInFirestore(device.id, {
        isOn: newOn,
        status: newOn ? "ON" : "OFF",
        power: newOn ? 12 : 0,
        current: (newOn && device.voltage > 0) ? 12 / device.voltage : 0
      });
    };

    toggleRec.onclick = () => {
      updateDeviceInFirestore(device.id, { recording: toggleRec.checked });
    };

    toggleMotion.onclick = () => {
      updateDeviceInFirestore(device.id, { motionDetection: toggleMotion.checked });
    };

    toggleNight.onclick = () => {
      updateDeviceInFirestore(device.id, { nightVision: toggleNight.checked });
    };
  }

  // Simulation Controls Listeners
  document.querySelectorAll(".btn-sim").forEach(btn => {
    btn.onclick = () => {
      const simStatus = btn.dataset.sim;
      let updates = { status: simStatus };

      if (simStatus === "ON") {
        updates.isOn = true;
        if (typeLower === "outlet") updates.power = 250;
        else if (typeLower === "light") updates.power = Math.round(((device.brightness || 80) * 15) / 100);
        else if (typeLower === "iron") {
          updates.timer = 120;
          updates.heating = true;
          updates.safetyMode = "SAFE";
          updates.power = calculateIronPower(device.temperature || 120);
        } else if (typeLower === "camera") updates.power = 12;
        else if (typeLower === "switch") {
          updates.switch1 = true;
          updates.switch2 = true;
          updates.switch3 = true;
          updates.power = 3;
        }
      } else if (simStatus === "OFF") {
        updates.isOn = false;
        updates.power = 0;
        updates.current = 0;
        if (typeLower === "iron") {
          updates.timer = 0;
          updates.heating = false;
        } else if (typeLower === "switch") {
          updates.switch1 = false;
          updates.switch2 = false;
          updates.switch3 = false;
        }
      } else if (simStatus === "ERROR" || simStatus === "DISCONNECTED") {
        updates.isOn = false;
        updates.power = 0;
        updates.current = 0;
        if (typeLower === "switch") {
          updates.switch1 = false;
          updates.switch2 = false;
          updates.switch3 = false;
        }
      }

      if (updates.power !== undefined && device.voltage > 0) {
        updates.current = updates.power / device.voltage;
      }

      updateDeviceInFirestore(device.id, updates);
    };
  });

  // Schedule Section Controls
  updateScheduleUI(device);
}

function calculateIronPower(temp) {
  if (temp <= 80) return 600;
  if (temp <= 120) return 900;
  if (temp <= 160) return 1200;
  if (temp <= 200) return 1500;
  return 1800;
}

// --------------------------------------------------------------------------
// 9. Scheduling Engine
// --------------------------------------------------------------------------
function updateScheduleUI(device) {
  const action = (device.scheduleAction || "").trim().toUpperCase();
  const dueAt = Number(device.scheduleDueAt || 0);

  if (dueAt > 0 && action) {
    const remainingMs = dueAt - Date.now();
    const seconds = Math.max(0, Math.ceil(remainingMs / 1000));
    lblScheduleStatus.textContent = `Schedule: ${action} in ${seconds}s`;
  } else {
    lblScheduleStatus.textContent = "Schedule: Not Set";
  }

  btnScheduleOn.onclick = () => {
    const dueAt = Date.now() + 10000;
    device.scheduleAction = "ON";
    device.scheduleDueAt = dueAt;
    device.scheduleRemaining = 10;
    lblScheduleStatus.textContent = "Schedule: ON in 10s";
    updateDeviceInFirestore(device.id, {
      scheduleAction: "ON",
      scheduleDueAt: dueAt,
      scheduleRemaining: 10
    });
  };

  btnScheduleOff.onclick = () => {
    const dueAt = Date.now() + 10000;
    device.scheduleAction = "OFF";
    device.scheduleDueAt = dueAt;
    device.scheduleRemaining = 10;
    lblScheduleStatus.textContent = "Schedule: OFF in 10s";
    updateDeviceInFirestore(device.id, {
      scheduleAction: "OFF",
      scheduleDueAt: dueAt,
      scheduleRemaining: 10
    });
  };

  btnScheduleCancel.onclick = () => {
    device.scheduleAction = "";
    device.scheduleDueAt = 0;
    device.scheduleRemaining = 0;
    lblScheduleStatus.textContent = "Schedule: Not Set";
    updateDeviceInFirestore(device.id, {
      scheduleAction: "",
      scheduleDueAt: 0,
      scheduleRemaining: 0
    });
  };
}

// --------------------------------------------------------------------------
// 10. Background Timer Engines (Iron Countdowns & Schedules)
// --------------------------------------------------------------------------
function processIronTimers() {
  // Check active irons that have timer > 0 and are ON
  devices.forEach(dev => {
    if ((dev.type || "").toLowerCase() === "iron" && dev.isOn && Number(dev.timer) > 0) {
      if (!ironLocalTimers[dev.id]) {
        ironLocalTimers[dev.id] = setInterval(() => {
          // Re-fetch current device state
          const currentDev = devices.find(d => d.id === dev.id);
          if (!currentDev || !currentDev.isOn || currentDev.timer <= 0) {
            clearInterval(ironLocalTimers[dev.id]);
            delete ironLocalTimers[dev.id];
            return;
          }

          const newTimer = currentDev.timer - 1;
          if (newTimer <= 0) {
            clearInterval(ironLocalTimers[dev.id]);
            delete ironLocalTimers[dev.id];
            // Auto Shutdown Safety Cutoff!
            updateDeviceInFirestore(dev.id, {
              isOn: false,
              status: "OFF",
              timer: 0,
              heating: false,
              power: 0,
              current: 0,
              safetyMode: "AUTO SHUTDOWN"
            });
          } else {
            // Update Firestore with countdown tick
            updateDeviceInFirestore(dev.id, { timer: newTimer });
          }
        }, 1000);
      }
    } else {
      if (ironLocalTimers[dev.id]) {
        clearInterval(ironLocalTimers[dev.id]);
        delete ironLocalTimers[dev.id];
      }
    }
  });
}

// Universal Schedule Engine Loop (runs every 500ms)
function startScheduleLoop() {
  if (scheduleInterval) clearInterval(scheduleInterval);
  scheduleInterval = setInterval(() => {
    const now = Date.now();
    devices.forEach(dev => {
      const dueAt = Number(dev.scheduleDueAt || 0);
      const action = (dev.scheduleAction || "").trim().toUpperCase();

      if (dueAt > 0 && action) {
        if (now >= dueAt) {
          // Schedule Due! Execute Action
          let updates = {
            scheduleAction: "",
            scheduleDueAt: 0,
            scheduleRemaining: 0
          };

          if (action === "ON") {
            updates.isOn = true;
            updates.status = "ON";
            const devType = (dev.type || "").toLowerCase();
            if (devType === "iron") {
              updates.timer = 120;
              updates.heating = true;
              updates.safetyMode = "SAFE";
              updates.power = calculateIronPower(dev.temperature || 120);
            } else if (devType === "light") {
              updates.power = Math.round(((dev.brightness || 80) * 15) / 100);
            } else if (devType === "switch") {
              updates.switch1 = true;
              updates.switch2 = true;
              updates.switch3 = true;
              updates.power = 3;
            } else if (devType === "outlet") {
              updates.power = 250;
            } else if (devType === "camera") {
              updates.power = 12;
            }
          } else if (action === "OFF") {
            updates.isOn = false;
            updates.status = "OFF";
            updates.power = 0;
            updates.current = 0;
            const devType = (dev.type || "").toLowerCase();
            if (devType === "iron") {
              updates.timer = 0;
              updates.heating = false;
            } else if (devType === "switch") {
              updates.switch1 = false;
              updates.switch2 = false;
              updates.switch3 = false;
            }
          }

          if (updates.power !== undefined && dev.voltage > 0) {
            updates.current = updates.power / dev.voltage;
          }

          updateDeviceInFirestore(dev.id, updates);
        } else {
          // Update local remaining display if modal open
          if (activeModalDeviceId === dev.id) {
            const seconds = Math.max(0, Math.ceil((dueAt - now) / 1000));
            lblScheduleStatus.textContent = `Schedule: ${action} in ${seconds}s`;
          }
        }
      } else {
        if (activeModalDeviceId === dev.id) {
          lblScheduleStatus.textContent = "Schedule: Not Set";
        }
      }
    });
  }, 500);
}

// Camera Live Timestamp Overlay Updater
function startCameraClock() {
  const tsElement = document.getElementById("cameraTimestamp");
  if (cameraClockInterval) clearInterval(cameraClockInterval);
  cameraClockInterval = setInterval(() => {
    if (tsElement && activeModalDeviceId) {
      const now = new Date();
      tsElement.textContent = now.toISOString().replace("T", " ").substring(0, 19);
    }
  }, 1000);
}

// --------------------------------------------------------------------------
// 11. Helper to Write Updates to Firestore
// --------------------------------------------------------------------------
function updateDeviceInFirestore(deviceId, updates) {
  db.collection("devices").doc(deviceId).update(updates)
    .then(() => {
      console.log(`Firestore updated for device ${deviceId}:`, updates);
    })
    .catch(err => {
      console.error(`Error updating device ${deviceId} in Firestore:`, err);
    });
}

// --------------------------------------------------------------------------
// 12. Application Entrypoint
// --------------------------------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
  initFirebaseListeners();
  startScheduleLoop();
  startCameraClock();
});
