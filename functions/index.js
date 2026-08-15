const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");

const {
    getFirestore,
    Timestamp
} = require("firebase-admin/firestore");

const {
    initializeApp
} = require("firebase-admin/app");

initializeApp();

const db = getFirestore();


// ============================================================
// DEVICE UPDATE LISTENER
// ============================================================
//
// Detects when a device is turned ON/OFF.
//
// For an Iron:
// ON  -> records when it was turned on
// OFF -> clears the ON timestamp
//
// ============================================================

exports.deviceStateListener = onDocumentUpdated(
    "devices/{deviceId}",
    async (event) => {

        const before = event.data.before.data();
        const after = event.data.after.data();

        if (!before || !after) {
            return;
        }

        const deviceRef = event.data.after.ref;

        // ----------------------------------------------------
        // DEVICE TURNED ON
        // ----------------------------------------------------

        if (
            before.isOn !== true &&
            after.isOn === true
        ) {

            console.log(
                `Device ${after.name} turned ON`
            );

            const updateData = {
                turnedOnAt: Timestamp.now()
            };

            // Iron-specific state
            if (
                String(after.type).toLowerCase() === "iron"
            ) {

                updateData.safetyMode = "SAFE";

                updateData.heating = true;
            }

            await deviceRef.update(updateData);

            return;
        }


        // ----------------------------------------------------
        // DEVICE TURNED OFF
        // ----------------------------------------------------

        if (
            before.isOn === true &&
            after.isOn !== true
        ) {

            console.log(
                `Device ${after.name} turned OFF`
            );

            await deviceRef.update({
                turnedOnAt: null
            });

            return;
        }
    }
);


// ============================================================
// IRON SAFETY WORKER
// ============================================================
//
// Runs every minute.
//
// Checks every Iron that is currently ON.
//
// If the maximum permitted ON duration is exceeded:
//      isOn       = false
//      status     = OFF
//      power      = 0
//      current    = 0
//      heating    = false
//      safetyMode = AUTO SHUTDOWN
//
// ============================================================

exports.ironSafetyWorker = onSchedule(
    {
        schedule: "every 1 minutes",
        timeZone: "Asia/Colombo"
    },
    async () => {

        console.log(
            "Running Iron safety worker..."
        );

        const snapshot = await db
            .collection("devices")
            .where("type", "==", "Iron")
            .where("isOn", "==", true)
            .get();


        if (snapshot.empty) {

            console.log(
                "No active irons found."
            );

            return;
        }


        const now = Date.now();


        for (const document of snapshot.docs) {

            const device = document.data();

            // ------------------------------------------------
            // MAXIMUM TIME
            // ------------------------------------------------

            const maxTime =
                Number(device.maxTime || 120);

            // maxTime is stored in minutes
            const maxDurationMilliseconds =
                maxTime * 60 * 1000;


            // ------------------------------------------------
            // TURNED ON TIME
            // ------------------------------------------------

            const turnedOnAt =
                device.turnedOnAt;


            if (!turnedOnAt) {

                console.log(
                    `${device.name}: missing turnedOnAt`
                );

                // Establish timestamp instead of shutting
                // the device down immediately.

                await document.ref.update({
                    turnedOnAt: Timestamp.now()
                });

                continue;
            }


            const turnedOnMilliseconds =
                turnedOnAt.toMillis();


            const elapsed =
                now - turnedOnMilliseconds;


            console.log(
                `${device.name}: ${elapsed} ms elapsed`
            );


            // ------------------------------------------------
            // SAFETY LIMIT BREACHED
            // ------------------------------------------------

            if (
                elapsed >=
                maxDurationMilliseconds
            ) {

                console.log(
                    `SAFETY CUTOFF: ${device.name}`
                );


                await document.ref.update({

                    // Main state
                    isOn: false,
                    status: "OFF",

                    // Timer
                    timer: 0,

                    // Heating
                    heating: false,

                    // Electrical state
                    power: 0,
                    current: 0,

                    // Safety state
                    safetyMode: "AUTO SHUTDOWN",

                    // Clear ON timestamp
                    turnedOnAt: null,

                    // Notification information
                    lastSafetyEvent:
                        "IRON_AUTO_SHUTDOWN",

                    lastSafetyEventAt:
                        Timestamp.now()
                });


                // ------------------------------------------------
                // CREATE NOTIFICATION
                // ------------------------------------------------

                await db
                    .collection("notifications")
                    .add({

                        type: "Iron",

                        title:
                            "Safety Shutdown",

                        message:
                            `${device.name} was automatically switched OFF because the maximum ON duration was exceeded.`,

                        deviceId:
                            document.id,

                        createdAt:
                            Timestamp.now(),

                        read: false
                    });
            }
        }
    }
);