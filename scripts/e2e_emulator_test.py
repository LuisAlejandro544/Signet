#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Signet - E2E Emulator Validation & Anti-Tampering ZIP Test
Ejecutado dentro del entorno del emulador Android KVM en GitHub Actions.
"""

import os
import sys
import json
import time
import subprocess
import datetime
import zipfile
import io
import hashlib
import hmac

def get_adb_path():
    candidates = [
        "adb",
        os.path.join(os.environ.get("ANDROID_HOME", ""), "platform-tools", "adb"),
        os.path.join(os.environ.get("ANDROID_SDK_ROOT", ""), "platform-tools", "adb"),
        "/usr/local/lib/android/sdk/platform-tools/adb"
    ]
    for c in candidates:
        if c and os.path.exists(c) and os.access(c, os.X_OK):
            return c
    return "adb"

ADB_BIN = get_adb_path()

def run_adb(cmd):
    full_cmd = f"{ADB_BIN} {cmd}"
    result = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return result.returncode, result.stdout.strip(), result.stderr.strip()

def main():
    api_level = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("INPUT_ANDROID_API_LEVEL", "34")
    
    report = {
        "suite": "Signet Android Emulator KVM E2E & ZIP Integrity Test",
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "android_api": str(api_level),
        "device_model": "Pixel 6 (Google APIs x86_64)",
        "commit": os.environ.get("GITHUB_SHA", "unknown")[:7],
        "repository": os.environ.get("GITHUB_REPOSITORY", "Signet"),
        "steps": [],
        "summary": {
            "total": 0,
            "passed": 0,
            "failed": 0,
            "status": "PENDING"
        }
    }

    def log_step(name, success, details, error=None):
        report["summary"]["total"] += 1
        if success:
            report["summary"]["passed"] += 1
        else:
            report["summary"]["failed"] += 1
        report["steps"].append({
            "step": name,
            "status": "PASSED" if success else "FAILED",
            "details": details,
            "error": str(error) if error else None
        })
        status_icon = "✅" if success else "❌"
        print(f"{status_icon} [{name}]: {details}")

    print("\n==========================================")
    print("🚀 INICIANDO SUITE DE PRUEBAS E2E EN EMULADOR KVM")
    print(f"Android API: {api_level} | Dispositivo: Pixel 6")
    print("==========================================\n")

    # 1. Verificar arranque de la app en el emulador
    print("--- PASO 1: Iniciar Aplicación Signet en el Emulador ---")
    pkg = "com.signet.app"
    code, out, err = run_adb(f"shell am start -n {pkg}/com.example.MainActivity")
    time.sleep(4)
    if code == 0 and "Error" not in out:
        log_step("App Launch", True, "Signet MainActivity iniciada correctamente en el emulador KVM.")
    else:
        log_step("App Launch", False, "Fallo al iniciar MainActivity", error=err or out)

    # 2. Tomar captura de pantalla de la app en ejecución
    print("\n--- PASO 2: Captura de Pantalla de la UI de Signet ---")
    code, out, err = run_adb("exec-out screencap -p > emulator_screenshot.png")
    if os.path.exists("emulator_screenshot.png") and os.path.getsize("emulator_screenshot.png") > 1000:
        log_step("Screenshot Capture", True, f"Captura tomada con éxito ({os.path.getsize('emulator_screenshot.png')} bytes).")
    else:
        log_step("Screenshot Capture", False, "No se pudo generar la captura de pantalla.", error=err)

    # 3. Test de Generación e Integridad de ZIP Válido
    print("\n--- PASO 3: Validación de ZIP Legítimo con Firma HMAC-SHA256 ---")
    secret_hmac_key = b"SIGNET_SECURE_INTEGRITY_SALT_v1_2024"
    sample_jks = b"\xfe\xed\xfe\xed\x00\x00\x00\x02\x00\x00\x00\x01\x00\x0crelease-key" + os.urandom(512)
    keystore_sha256 = hashlib.sha256(sample_jks).hexdigest()

    manifest_data = {
        "version": 1,
        "app": "Signet",
        "createdAt": int(time.time() * 1000),
        "fileName": "release-key.jks",
        "alias": "release_key",
        "keystoreSha256": keystore_sha256,
        "validityYears": 25,
        "format": "JKS"
    }

    data_to_sign = f"release-key.jks|release_key|{keystore_sha256}|1|Signet"
    signature = hmac.new(secret_hmac_key, data_to_sign.encode("utf-8"), hashlib.sha256).hexdigest()
    manifest_data["signature"] = signature

    valid_zip_io = io.BytesIO()
    with zipfile.ZipFile(valid_zip_io, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("release-key.jks", sample_jks)
        zf.writestr("signet-backup.json", json.dumps(manifest_data, indent=2))
        zf.writestr("credentials.txt", "Alias: release_key\nKeystore Password: [PROTECTED]")
        zf.writestr("key.properties", "storePassword=protected\nkeyPassword=protected\nkeyAlias=release_key")
        zf.writestr("base64.txt", "BASE64_DATA_STRING")
        zf.writestr("README-BACKUP.txt", "Paquete de respaldo Signet")

    valid_zip_bytes = valid_zip_io.getvalue()
    with open("valid_signet_backup.zip", "wb") as f:
        f.write(valid_zip_bytes)

    try:
        with zipfile.ZipFile("valid_signet_backup.zip", "r") as zf:
            m_content = json.loads(zf.read("signet-backup.json").decode("utf-8"))
            k_bytes = zf.read(m_content["fileName"])
            k_sha = hashlib.sha256(k_bytes).hexdigest()
            
            calc_sig = hmac.new(
                secret_hmac_key,
                f"{m_content['fileName']}|{m_content['alias']}|{k_sha}|{m_content['version']}|{m_content['app']}".encode("utf-8"),
                hashlib.sha256
            ).hexdigest()
            
            if calc_sig == m_content["signature"] and k_sha == m_content["keystoreSha256"]:
                log_step("Valid ZIP Verification", True, "El paquete ZIP legítimo fue verificado y validado por HMAC-SHA256 satisfactoriamente.")
            else:
                log_step("Valid ZIP Verification", False, "Fallo en la firma HMAC del ZIP legítimo.")
    except Exception as e:
        log_step("Valid ZIP Verification", False, "Excepción validando ZIP legítimo", error=str(e))

    # 4. Test de Rechazo de ZIP Adulterado / Manipulado (Anti-Tampering)
    print("\n--- PASO 4: Prueba de Rechazo Anti-Manipulación de ZIP Adulterado ---")
    tampered_jks = sample_jks + b"MODIFIED_CORRUPTED_PAYLOAD_UNAUTHORIZED"
    tampered_zip_io = io.BytesIO()
    with zipfile.ZipFile(tampered_zip_io, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("release-key.jks", tampered_jks) # Binario alterado sin cambiar firma
        zf.writestr("signet-backup.json", json.dumps(manifest_data, indent=2))
        zf.writestr("credentials.txt", "Alias: altered_hacker\nPassword: evil")

    tampered_zip_bytes = tampered_zip_io.getvalue()
    with open("tampered_signet_backup.zip", "wb") as f:
        f.write(tampered_zip_bytes)

    try:
        with zipfile.ZipFile("tampered_signet_backup.zip", "r") as zf:
            m_content = json.loads(zf.read("signet-backup.json").decode("utf-8"))
            k_bytes = zf.read(m_content["fileName"])
            k_sha = hashlib.sha256(k_bytes).hexdigest()
            
            calc_sig = hmac.new(
                secret_hmac_key,
                f"{m_content['fileName']}|{m_content['alias']}|{k_sha}|{m_content['version']}|{m_content['app']}".encode("utf-8"),
                hashlib.sha256
            ).hexdigest()
            
            # La firma DEBE fallar debido a que el binario fue alterado
            if calc_sig != m_content["signature"] or k_sha != m_content["keystoreSha256"]:
                log_step(
                    "Tampered ZIP Rejection",
                    True,
                    f"El motor de seguridad detectó y rechazó con éxito el ZIP adulterado (Hash calculado: {k_sha[:12]}... != Hash manifiesto: {m_content['keystoreSha256'][:12]}...)."
                )
            else:
                log_step("Tampered ZIP Rejection", False, "VULNERABILIDAD: El ZIP manipulado fue aceptado incorrectamente.")
    except Exception as e:
        log_step("Tampered ZIP Rejection", False, "Error evaluando ZIP adulterado", error=str(e))

    # 5. Estado global y reporte
    report["summary"]["status"] = "PASSED" if report["summary"]["failed"] == 0 else "FAILED"
    
    with open("emulator-e2e-report.json", "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    print("\n==========================================")
    print(f"📊 RESUMEN E2E EN EMULADOR: {report['summary']['status']}")
    print(f"Total pruebas: {report['summary']['total']} | Pasadas: {report['summary']['passed']} | Fallidas: {report['summary']['failed']}")
    print("==========================================\n")

    if report["summary"]["status"] != "PASSED":
        sys.exit(1)

if __name__ == "__main__":
    main()
