import unittest, tempfile, threading, json, urllib.request, urllib.error
from pathlib import Path
import server
class ApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp=tempfile.TemporaryDirectory(); server.STATE=Path(cls.temp.name)/'state.json'
        cls.http=server.ThreadingHTTPServer(('127.0.0.1',0),server.Handler)
        threading.Thread(target=cls.http.serve_forever,daemon=True).start()
        cls.base='http://127.0.0.1:'+str(cls.http.server_port)
    @classmethod
    def tearDownClass(cls): cls.http.shutdown(); cls.http.server_close(); cls.temp.cleanup()
    def setUp(self):
        server.save(server.seed()); server.SESSIONS.clear()
    def call(self,path,method='GET',body=None,token=''):
        r=urllib.request.Request(self.base+path,data=json.dumps(body).encode() if body is not None else None,method=method,headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'})
        try:
            with urllib.request.urlopen(r) as resp: return resp.status,json.load(resp)
        except urllib.error.HTTPError as e: return e.code,json.load(e)
    def login(self,role): return self.call('/login','POST',{'username':role,'password':'Demo2026!'})[1]['token']
    def test_authentication(self):
        self.assertEqual(self.call('/snapshot')[0],401)
        self.assertEqual(self.call('/login','POST',{'username':'administrador','password':'bad'})[0],401)
    def test_sales_and_transition_conflict(self):
        t=self.login('repartidor')
        self.assertEqual(self.call('/orders/1','PATCH',{'status':'Entregado','expectedStatus':'Pendiente'},t)[0],400)
        self.assertEqual(self.call('/orders/1','PATCH',{'status':'En ruta','expectedStatus':'Pendiente'},t)[0],200)
        self.assertEqual(self.call('/orders/1','PATCH',{'status':'Entregado','expectedStatus':'Pendiente'},t)[0],409)
        self.assertEqual(self.call('/orders/1','PATCH',{'status':'Entregado','expectedStatus':'En ruta'},t)[0],200)
        snap=self.call('/snapshot',token=t)[1]
        self.assertEqual(snap['metrics']['sales'],24000); self.assertEqual(len(snap['orders'][0]['history']),3)
    def test_permissions_and_validation(self):
        body={'clientId':'1','productId':'2','quantity':3,'priority':'Alta'}
        self.assertEqual(self.call('/orders','POST',body,self.login('supervisor'))[0],403)
        t=self.login('vendedor'); code,o=self.call('/orders','POST',body,t)
        self.assertEqual(code,201); self.assertEqual(o['total'],48000)
        body['quantity']=-1; self.assertEqual(self.call('/orders','POST',body,t)[0],400)
        self.assertEqual(self.call('/users/4','PATCH',{'role':'Administrador','active':True},t)[0],403)
    def test_assignment_and_revocation(self):
        t=self.login('repartidor'); d=server.read(); d['routes'][0]['driverId']='other'; server.save(d)
        self.assertEqual(self.call('/snapshot',token=t)[1]['orders'],[])
        self.assertEqual(self.call('/orders/1','PATCH',{'status':'En ruta','expectedStatus':'Pendiente'},t)[0],404)
        a=self.login('administrador'); self.call('/users/4','PATCH',{'role':'Repartidor','active':False},a)
        self.assertEqual(self.call('/snapshot',token=t)[0],401)
    def test_incident_and_logout(self):
        t=self.login('repartidor')
        self.call('/orders/1','PATCH',{'status':'En ruta','expectedStatus':'Pendiente'},t)
        self.assertEqual(self.call('/orders/1','PATCH',{'status':'Incidencia','expectedStatus':'En ruta'},t)[0],400)
        self.assertEqual(self.call('/orders/1','PATCH',{'status':'Incidencia','expectedStatus':'En ruta','note':'Dirección ficticia no disponible'},t)[0],200)
        self.call('/logout','POST',{},t); self.assertEqual(self.call('/snapshot',token=t)[0],401)
    def test_persistence(self):
        t=self.login('vendedor'); self.call('/orders','POST',{'clientId':'2','productId':'1','quantity':1,'priority':'Normal'},t)
        self.assertEqual(len(json.loads(server.STATE.read_text(encoding='utf-8'))['orders']),2)
if __name__=='__main__': unittest.main(verbosity=2)
