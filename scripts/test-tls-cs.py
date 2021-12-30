#!/usr/bin/env python3
import re
import time

import pandas as pd
import subprocess

MODES = ['PROXY ONLY', 'SIGNALING ONLY', 'MUTUAL']
CONFIGS = ['config/proxybox_tls.properties', 'config/signalingserver_tls.properties']


def write_conf(mode, version, ciphersuite):
    for file_name in CONFIGS:
        f = open(file_name, 'w')
        f.write('# options are: MUTUAL, PROXY ONLY, SIGNALING ONLY\n')
        f.write(f'authentication={mode}\n')
        f.write(f'# options are: TLSv1.1, TLSv1.2, TLSv1.3\n')
        f.write(f'tlsversion={version}\n')
        f.write('# list of ciphersuites\n')
        f.write(f'ciphersuites={ciphersuite}\n')
        f.flush()
        f.close()

def string_found(string1, string2):
   if re.search(r"\b" + re.escape(string1) + r"\b", string2):
      return True
   return False


def test_suite(m ,v, cs):
    write_conf(m, v, cs)
    try:
        byte_output = subprocess.check_output('./scripts/run-pa2-sapkdp.sh cars resources/coin_3040021e45931ef.voucher', shell=True, stderr=subprocess.STDOUT)
        if len(byte_output) != 0:
            return False, byte_output.decode().split('\n')[0]
        return True, _
    except subprocess.CalledProcessError as exc:
        print(exc)
        return False, exc


if __name__ == '__main__':
    dataframe = pd.read_csv('config/ciphersuites_tls.csv')

    for _, row in dataframe.iterrows():
        version = row["version"]
        ciphersuite = row["ciphersuite"]

        for mode in MODES:
            ok, err = test_suite(mode, version, ciphersuite)
            if ok:
                print(f'{mode} - {version} {ciphersuite} - OK')
            else:
                print(f'{mode} - {version} {ciphersuite} - FAILED with {err}')
