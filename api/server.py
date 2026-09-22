"""API académica: solo datos sintéticos. Python 3, sin dependencias externas."""
import json, secrets, threading, os
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROLES = ['Administrador', 'Supervisor', 'Vendedor', 'Repartidor']
STATE = Path(os.environ.get('DEMO_STATE', str(Path(__file__).with_name('state.json'))))
LOCK = threading.RLock()
SESSIONS = {}
def now(): return datetime.now(timezone.utc).isoformat()
def seed():
    return dict(users=[dict(id=str(i+1), name=r+' demo', username=r.lower(), role=r, active=True) for i,r in enumerate(ROLES)],
        clients=[dict(id='1', name='Almacén Demo Centro', address='Calle Ficticia 100, Ciudad Demo'), dict(id='2', name='Cliente Sintético Sur', address='Pasaje de Prueba 200, Ciudad Demo')],
        products=[dict(id='1', name='Producto de prueba A', price=12000),dict(id='2',name='Producto de prueba B',price=16000)],
        routes=[dict(id='1', name='Ruta Centro · simulada', driverId='4', vehicle='DEMO-01', branch='Sucursal Ciudad Demo', schedule='09:00–18:00', location='-33.450, -70.660 (referencial)')],
        orders=[dict(id='1',clientId='1',productId='1',quantity=2,total=24000,priority='Normal',status='Pendiente',routeId='1',date=now(), history=[dict(status='Pendiente', date=now(), actor='Sistema demo')])])
def read(): return json.loads(STATE.read_text(encoding='utf-8')) if STATE.exists() else seed()
def save(d):
    tmp=STATE.with_suffix('.tmp'); tmp.write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8'); tmp.replace(STATE)
class ApiError(Exception):
    def __init__(self, status, message): self.status,self.message=status,message
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args): pass
    def reply(self, status, data):
        b=json.dumps(data,ensure_ascii=False).encode(); self.send_response(status); self.send_header('Content-Type','application/json; charset=utf-8'); self.send_header('Content-Length',str(len(b))); self.end_headers(); self.wfile.write(b)
    def do_GET(self): self.handle_api()
    def do_POST(self): self.handle_api()
    def do_PATCH(self): self.handle_api()
    def handle_api(self):
        try:
            with LOCK:
                d=read(); n=int(self.headers.get('Content-Length','0'))
                if n<0 or n>16384: raise ApiError(413,'Tamaño de solicitud inválido')
                body=json.loads(self.rfile.read(n)) if n else {}
                if not isinstance(body,dict): raise ApiError(400,'Se requiere un objeto JSON')
                path=self.path.split('?')[0]
                if path=='/health' and self.command=='GET': return self.reply(200,{'status':'ok'})
                if path=='/login' and self.command=='POST':
                    u=next((u for u in d['users'] if u['username']==body.get('username') and u['active']),None)
                    if not u or body.get('password')!='Demo2026!': raise ApiError(401,'Usuario o contraseña incorrectos')
                    token=secrets.token_urlsafe(32); SESSIONS[token]=u['id']; return self.reply(200,dict(token=token,user=u))
                token=self.headers.get('Authorization','').removeprefix('Bearer ')
                u=next((u for u in d['users'] if u['id']==SESSIONS.get(token) and u['active']),None)
                if not u: raise ApiError(401,'Inicia sesión nuevamente')
                role=u['role']
                if path=='/logout' and self.command=='POST': SESSIONS.pop(token,None); return self.reply(200,{'ok':True})
                routes=[r for r in d['routes'] if role!='Repartidor' or r['driverId']==u['id']]
                orders=[o for o in d['orders'] if role!='Repartidor' or o['routeId'] in [r['id'] for r in routes]]
                if path=='/snapshot' and self.command=='GET':
                    delivered=[o for o in orders if o['status']=='Entregado']
                    sales=sum(o['total'] for o in delivered)
                    return self.reply(200,dict(user=u,clients=[c for c in d['clients'] if role!='Repartidor' or c['id'] in [o['clientId'] for o in orders]],products=d['products'],orders=orders,routes=routes,users=d['users'] if role=='Administrador' else [],metrics=dict(sales=sales,goal=200000,delivered=len(delivered),pending=len(orders)-len(delivered)),syncedAt=now()))
                if path=='/orders' and self.command=='POST':
                    if role not in ['Administrador','Vendedor']: raise ApiError(403,'Este rol no puede registrar pedidos')
                    p=next((p for p in d['products'] if p['id']==body.get('productId')),None)
                    qty=body.get('quantity'); priority=body.get('priority')
                    if not p or not any(c['id']==body.get('clientId') for c in d['clients']) or type(qty)!=int or not 1<=qty<=100 or priority not in ['Normal','Alta']: raise ApiError(400,'Revisa cliente, producto, cantidad (1–100) y prioridad')
                    o=dict(id=str(max([int(o['id']) for o in d['orders']]+[0])+1),clientId=body['clientId'],productId=p['id'],quantity=qty,total=p['price']*qty,priority=priority,status='Pendiente',routeId='1',date=now(),history=[dict(status='Pendiente',date=now(),actor=u['name'])]); d['orders'].append(o); save(d); return self.reply(201,o)
                if path.startswith('/orders/') and self.command=='PATCH':
                    if role not in ['Administrador','Repartidor']: raise ApiError(403,'Sin permiso para actualizar entregas')
                    o=next((o for o in orders if o['id']==path.split('/')[-1]),None)
                    if not o: raise ApiError(404,'Pedido no encontrado o no asignado')
                    if body.get('expectedStatus')!=o['status']: raise ApiError(409,'El pedido cambió. Actualiza antes de continuar')
                    allowed={'Pendiente':['En ruta'],'En ruta':['Entregado','Incidencia'],'Incidencia':['En ruta'],'Entregado':[]}
                    status=body.get('status')
                    if status not in allowed[o['status']]: raise ApiError(400,'Transición de estado inválida')
                    note=str(body.get('note','')).strip()
                    if status=='Incidencia' and not note: raise ApiError(400,'Describe la incidencia')
                    o['status']=status; o['history'].append(dict(status=status,date=now(),actor=u['name'],note=note[:500])); save(d); return self.reply(200,o)
                if path.startswith('/users/') and self.command=='PATCH':
                    if role!='Administrador': raise ApiError(403,'Solo administrador')
                    target=next((x for x in d['users'] if x['id']==path.split('/')[-1]),None)
                    if not target: raise ApiError(404,'Usuario no encontrado')
                    if target['id']==u['id']: raise ApiError(400,'No puedes modificar tu propio acceso')
                    if body.get('role') not in ROLES or type(body.get('active'))!=bool: raise ApiError(400,'Rol o estado inválido')
                    target.update(role=body['role'],active=body['active']); save(d); return self.reply(200,target)
                raise ApiError(404,'Recurso no encontrado')
        except ApiError as e: self.reply(e.status,{'error':e.message})
        except (ValueError,TypeError,KeyError): self.reply(400,{'error':'Solicitud inválida'})
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser(); p.add_argument('--port',type=int,default=8000); a=p.parse_args()
    print(f'API de prueba en http://127.0.0.1:{a.port}',flush=True)
    ThreadingHTTPServer(('127.0.0.1',a.port),Handler).serve_forever()
