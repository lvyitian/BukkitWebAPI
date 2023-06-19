package top.dsbbs2.wa;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main extends JavaPlugin {
    public static Main instance;
    {
        instance=this;
    }
    public static final HttpServer server;
    public static final Gson gson=new GsonBuilder().enableComplexMapKeySerialization().setLenient().setPrettyPrinting().create();
    static{
        try {
            server = HttpServer.create(new InetSocketAddress(7588),4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.createContext("/list",ctx->{
            ctx.getResponseHeaders().add("Content-Type","application/json; charset=utf-8");
            if(instance==null)
            {
                try(OutputStream o=ctx.getResponseBody()){
                    JsonObject obj=new JsonObject();
                    obj.add("code",new JsonPrimitive(-3));
                    obj.add("err",new JsonPrimitive("Plugin is not loaded"));
                    o.write(gson.toJson(obj).getBytes(StandardCharsets.UTF_8));
                }
                return;
            }
            boolean[] completed=new boolean[]{false};
            Bukkit.getScheduler().runTask(instance,()->{
                try {
                    try (OutputStream o = ctx.getResponseBody()) {
                        o.write(gson.toJson(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), new TypeToken<List<String>>() {
                        }.getType()).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                }finally{completed[0]=true;}
            });
            while(!completed[0]){try{Thread.sleep(1);}catch(InterruptedException exc){throw new RuntimeException(exc);}}
        });
        server.createContext("/inventory",ctx->{
            ctx.getResponseHeaders().add("Content-Type","application/json; charset=utf-8");
            String path=ctx.getRequestURI().getRawPath();
            if(path.endsWith("/")) path=path.substring(0,path.length()-1);
            path=path.substring(10);
            if(path.startsWith("/")) path=path.substring(1);
            if(path.trim().isEmpty())
            {
                try(OutputStream o=ctx.getResponseBody()){
                    JsonObject obj=new JsonObject();
                    obj.add("code",new JsonPrimitive(-1));
                    obj.add("err",new JsonPrimitive("Argument <player> is mandatory"));
                    o.write(gson.toJson(obj).getBytes(StandardCharsets.UTF_8));
                }
                return;
            }else{
                if(instance==null)
                {
                    try(OutputStream o=ctx.getResponseBody()){
                        JsonObject obj=new JsonObject();
                        obj.add("code",new JsonPrimitive(-3));
                        obj.add("err",new JsonPrimitive("Plugin is not loaded"));
                        o.write(gson.toJson(obj).getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }
                boolean[] completed=new boolean[]{false};
                String path2=path;
                Bukkit.getScheduler().runTask(instance,()-> {
                    try {
                        Player p=Bukkit.getPlayer(path2);
                        if(p==null)
                        {
                            try(OutputStream o=ctx.getResponseBody()){
                                JsonObject obj=new JsonObject();
                                obj.add("code",new JsonPrimitive(-2));
                                obj.add("err",new JsonPrimitive("Player \""+path2+"\" is not online"));
                                o.write(gson.toJson(obj).getBytes(StandardCharsets.UTF_8));
                            }catch (IOException exc) {
                                throw new RuntimeException(exc);
                            }
                            return;
                        }else{
                            ItemStack[] c=p.getInventory().getContents();
                            List<WebItemStack> inv=new ArrayList<>(c.length);
                            Stream.of(c).map(i->{
                                WebItemStack wis=new WebItemStack();
                                wis.amount=i.getAmount();
                                wis.type=i.getType().name();
                                wis.customName=i.getItemMeta().getDisplayName();
                                wis.enchantments=i.getEnchantments().entrySet().parallelStream().map(i2->new AbstractMap.SimpleEntry<>(i2.getKey().getName(),(long)(int)i2.getValue())).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
                                return wis;
                            }).forEach(inv::add);
                            try(OutputStream o=ctx.getResponseBody()){
                                JsonObject obj=new JsonObject();
                                obj.add("code",new JsonPrimitive(0));
                                obj.add("selectedSlot",new JsonPrimitive(p.getInventory().getHeldItemSlot()));
                                obj.add("inventory",gson.toJsonTree(inv,new TypeToken<List<WebItemStack>>(){}.getType()));
                                o.write(gson.toJson(obj).getBytes(StandardCharsets.UTF_8));
                            }catch (IOException exc) {
                                throw new RuntimeException(exc);
                            }
                            return;
                        }
                    }finally{completed[0]=true;}
                    });
                while(!completed[0]){try{Thread.sleep(1);}catch(InterruptedException exc){throw new RuntimeException(exc);}}
            }
        });
    }
    public static class WebItemStack
    {
        public int amount=1;
        public String type="minecraft:air";
        public String customName="";
        public Map<String,Long> enchantments=new HashMap<>();
    }
    @Override
    public void onLoad()
    {
        if(instance==null) throw new RuntimeException("Reloading is not supported!");
    }
    @Override
    public void onEnable()
    {
        if(instance==null) throw new RuntimeException("Reloading is not supported!");
    }
    @Override
    public void onDisable()
    {
        instance=null;
        if(server!=null) server.stop(0);
    }
}
