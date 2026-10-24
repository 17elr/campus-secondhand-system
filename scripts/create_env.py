"""Generate local Compose credentials without overwriting existing configuration."""
from pathlib import Path
import secrets

def main():
    path=Path(__file__).resolve().parents[1]/'.env'
    values={key:secrets.token_urlsafe(36) for key in ('MYSQL_ROOT_PASSWORD','MARKET_DB_PASSWORD','NOTICE_DB_PASSWORD','REDIS_PASSWORD','NOTICE_INTERNAL_TOKEN')}
    with path.open('x',encoding='utf-8') as out:
        out.write(''.join(f'{key}={value}\n' for key,value in values.items())+'MARKET_PROFILES=\nCOOKIE_SECURE=false\n')
    print('Created .env; credentials were not printed. Set MARKET_PROFILES=demo only for an empty demo database.')
if __name__=='__main__':main()
