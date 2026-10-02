
import os
import glob
import json

mixin_json_files = glob.glob("**/*map6b6t.mixins.json", recursive=True)
for f in mixin_json_files:
    with open(f, "r") as file:
        data = json.load(file)
    
    if "client" in data and "ClientPlayNetworkHandlerMixin" in data["client"]:
        data["client"].remove("ClientPlayNetworkHandlerMixin")
        with open(f, "w") as file:
            json.dump(data, file, indent=2)
        print(f"Updated {f}")

